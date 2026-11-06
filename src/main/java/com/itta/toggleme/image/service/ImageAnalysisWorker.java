package com.itta.toggleme.image.service;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisResult;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

// 분석 executor 스레드에서 실행된다. Gemini 호출이 수 초 걸려 트랜잭션 밖에서 처리하고 결과만 저장한다
@Slf4j
@Component
public class ImageAnalysisWorker {

    private static final String PROMPT = """
            사용자가 자기 취향을 기록하려고 올린 사진이다. 책, 화장품, 음식, 옷, 영화, 물건 등 무엇이든 될 수 있다.
            사진 속 대상의 색상, 분위기, 스타일, 종류·대상을 보고 기록에 붙일 한국어 태그 3개를 만든다.
            - 각 태그는 #으로 시작하고 15자 이내
            - 사진에서 알아볼 수 있는 구체적인 특징을 우선한다 (예: 책이면 장르·작가, 음식이면 메뉴)
            - searchQueries는 아래에 따로 지시하지 않으면 빈 목록으로 둔다
            """;

    private final ChatClient chatClient;
    private final ImageService imageService;
    private final ImageAnalysisRepository imageAnalysisRepository;

    public ImageAnalysisWorker(
            ChatClient.Builder chatClientBuilder,
            ImageService imageService,
            ImageAnalysisRepository imageAnalysisRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.imageService = imageService;
        this.imageAnalysisRepository = imageAnalysisRepository;
    }

    // Gemini는 HEIC를 URL로 받지 않는다. 키 확장자는 ImageService가 업로드 형식으로 정한다
    public static boolean sendsBytes(String imageKey) {
        return imageKey.endsWith(".heic");
    }

    // 공통 규칙 뒤에 가구별 지침을 한 줄 덧붙인다. 지침이 없는 가구나 가구 없는 요청은 공통 규칙만 쓴다
    static String promptFor(FurnitureType furnitureType) {
        return FurnitureTagGuide.forCode(furnitureType == null ? null : furnitureType.getCode())
                .map(guide -> PROMPT + guide + "\n")
                .orElse(PROMPT);
    }

    public void analyze(Long analysisId) {
        ImageAnalysis analysis = imageAnalysisRepository.findByIdWithFurnitureType(analysisId).orElse(null);
        if (analysis == null) {
            log.warn("Image analysis {} not found", analysisId);
            return;
        }
        // 대기열에서 60초를 넘긴 요청은 앱이 이미 포기했으므로 Gemini를 호출하지 않는다
        if (analysis.currentStatus(Instant.now()) == ImageAnalysisStatus.FAILED) {
            analysis.fail();
            imageAnalysisRepository.save(analysis);
            return;
        }
        String imageKey = analysis.getImageKey();
        String prompt = promptFor(analysis.getFurnitureType());
        try {
            ImageAnalysisResult result = sendsBytes(imageKey)
                    ? analyzeBytes(imageKey, prompt)
                    : analyzeUrl(imageKey, prompt);
            // 모델이 빈 응답을 주면 결과 없는 COMPLETED가 되지 않게 실패로 둔다. 앱은 태그 없이 기록을 이어간다
            if (result == null) {
                log.warn("Image analysis {} failed: empty result", analysisId);
                analysis.fail();
            } else {
                // 검색어는 거울에서만 쓴다. 다른 가구는 모델이 채워도 버린다
                FurnitureType furnitureType = analysis.getFurnitureType();
                boolean keepsSearchQueries = furnitureType != null
                        && FurnitureTagGuide.createsSearchQueries(furnitureType.getCode());
                analysis.complete(keepsSearchQueries ? result : result.withoutSearchQueries());
            }
        } catch (RuntimeException | MalformedURLException e) {
            log.warn("Image analysis {} failed: {}", analysisId, e.getClass().getSimpleName());
            analysis.fail();
        }
        imageAnalysisRepository.save(analysis);
    }

    private ImageAnalysisResult analyzeUrl(String imageKey, String prompt) throws MalformedURLException {
        MimeType mimeType = MimeType.valueOf(imageKey.endsWith(".png") ? "image/png" : "image/jpeg");
        var imageUrl = URI.create(imageService.createViewUrl(imageKey)).toURL();
        return chatClient.prompt()
                .user(user -> user.text(prompt).media(mimeType, imageUrl))
                .call()
                .entity(ImageAnalysisResult.class);
    }

    private ImageAnalysisResult analyzeBytes(String imageKey, String prompt) {
        ResponseBytes<GetObjectResponse> image = imageService.download(imageKey);
        return chatClient.prompt()
                .user(user -> user.text(prompt).media(
                        MimeType.valueOf(image.response().contentType()),
                        new ByteArrayResource(image.asByteArray())))
                .call()
                .entity(ImageAnalysisResult.class);
    }
}

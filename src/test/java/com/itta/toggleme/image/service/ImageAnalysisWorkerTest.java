package com.itta.toggleme.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisResult;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

class ImageAnalysisWorkerTest {

    private final ImageService imageService = mock(ImageService.class);
    private final ImageAnalysisRepository imageAnalysisRepository = mock(ImageAnalysisRepository.class);
    private final ImageAnalysisWorker imageAnalysisWorker = new ImageAnalysisWorker(
            mock(ChatClient.Builder.class), imageService, imageAnalysisRepository);

    @Test
    void 분석_중_오류가_나면_실패로_저장한다() {
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.heic", null);
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.now());
        given(imageAnalysisRepository.findByIdWithFurnitureType(1L)).willReturn(Optional.of(analysis));
        given(imageService.download("images/7/a.heic")).willThrow(new IllegalStateException("S3 down"));

        imageAnalysisWorker.analyze(1L);

        assertThat(analysis.getStatus()).isEqualTo(ImageAnalysisStatus.FAILED);
        verify(imageAnalysisRepository).save(analysis);
    }

    @Test
    void 모델이_빈_응답을_주면_실패로_저장한다() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        given(builder.build()).willReturn(chatClient);
        given(chatClient.prompt().user(any(Consumer.class)).call().entity(ImageAnalysisResult.class)).willReturn(null);
        ImageAnalysisWorker worker = new ImageAnalysisWorker(builder, imageService, imageAnalysisRepository);
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.jpg", null);
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.now());
        given(imageAnalysisRepository.findByIdWithFurnitureType(1L)).willReturn(Optional.of(analysis));
        given(imageService.createViewUrl("images/7/a.jpg")).willReturn("https://view/images/7/a.jpg");

        worker.analyze(1L);

        assertThat(analysis.getStatus()).isEqualTo(ImageAnalysisStatus.FAILED);
        assertThat(analysis.getResult()).isNull();
    }

    @Test
    void 가구별_지침이_있으면_공통_규칙_뒤에_붙인다() {
        String prompt = ImageAnalysisWorker.promptFor(FurnitureType.create("fridge", "냉장고", 1));

        assertThat(prompt).contains("태그 3개").contains("음식에 집중");
        assertThat(ImageAnalysisWorker.promptFor(FurnitureType.create("mirror", "거울", 2))).contains("입은 옷에 집중");
    }

    @Test
    void 거울_지침에는_검색어_규칙이_있고_다른_가구에는_없다() {
        String mirror = ImageAnalysisWorker.promptFor(FurnitureType.create("mirror", "거울", 2));

        assertThat(mirror).contains("searchQueries").contains("계절, 색, 핏, 옷 종류 순서").contains("브랜드명").contains("20자 이내");
        assertThat(ImageAnalysisWorker.promptFor(FurnitureType.create("fridge", "냉장고", 1))).doesNotContain("쇼핑몰");
    }

    @Test
    void 검색어는_거울에서만_저장하고_다른_가구는_버린다() {
        ImageAnalysisResult modelOutput = new ImageAnalysisResult(List.of("#데일리"), List.of("가을 베이지 코트"));

        assertThat(analyzeWith("mirror", modelOutput).searchQueries()).containsExactly("가을 베이지 코트");
        assertThat(analyzeWith("fridge", modelOutput).searchQueries()).isEmpty();
        assertThat(analyzeWith("fridge", modelOutput).tags()).containsExactly("#데일리");
        assertThat(analyzeWith(null, modelOutput).searchQueries()).isEmpty();
    }

    private ImageAnalysisResult analyzeWith(String furnitureCode, ImageAnalysisResult modelOutput) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        given(builder.build()).willReturn(chatClient);
        given(chatClient.prompt().user(any(Consumer.class)).call().entity(ImageAnalysisResult.class)).willReturn(modelOutput);
        ImageAnalysisWorker worker = new ImageAnalysisWorker(builder, imageService, imageAnalysisRepository);
        FurnitureType furnitureType = furnitureCode == null ? null : FurnitureType.create(furnitureCode, "가구", 1);
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.jpg", furnitureType);
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.now());
        given(imageAnalysisRepository.findByIdWithFurnitureType(1L)).willReturn(Optional.of(analysis));
        given(imageService.createViewUrl("images/7/a.jpg")).willReturn("https://view/images/7/a.jpg");

        worker.analyze(1L);

        assertThat(analysis.getStatus()).isEqualTo(ImageAnalysisStatus.COMPLETED);
        return analysis.getResult();
    }

    @Test
    void 지침이_없는_가구나_가구_없는_요청은_공통_규칙만_쓴다() {
        String common = ImageAnalysisWorker.promptFor(null);

        assertThat(common).doesNotContain("집중");
        assertThat(ImageAnalysisWorker.promptFor(FurnitureType.create("desk", "책상", 3))).isEqualTo(common);
    }
}

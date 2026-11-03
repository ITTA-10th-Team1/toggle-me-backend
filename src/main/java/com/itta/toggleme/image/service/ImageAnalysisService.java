package com.itta.toggleme.image.service;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.dto.ImageAnalysisResponse;
import com.itta.toggleme.image.exception.ImageAnalysisBusyException;
import com.itta.toggleme.image.exception.ImageAnalysisNotFoundException;
import com.itta.toggleme.image.exception.ImageAnalysisTooManyException;
import com.itta.toggleme.image.exception.ImageInvalidException;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.exception.FurnitureTypeNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageAnalysisService {

    // 기록 화면은 사진을 한 장씩 올려서 정상 사용에서는 닿지 않는다
    private static final int MAX_PENDING_PER_MEMBER = 3;

    private final ImageService imageService;
    private final ImageAnalysisWorker imageAnalysisWorker;
    private final ImageAnalysisRepository imageAnalysisRepository;
    private final MemberRepository memberRepository;
    private final FurnitureTypeRepository furnitureTypeRepository;
    private final ThreadPoolTaskExecutor urlImageAnalysisExecutor;
    private final ThreadPoolTaskExecutor inlineImageAnalysisExecutor;

    // ponytail: 대기열은 메모리에만 있어 서버가 재시작되면 남은 요청은 60초 뒤 실패로 보인다.
    // 재시작에도 이어서 처리해야 하면 PENDING 행을 주기적으로 다시 꺼내는 방식으로 바꾼다
    @Transactional
    public ImageAnalysisResponse requestAnalysis(Long memberId, String imageKey, Long furnitureTypeId) {
        if (!imageService.isUploadedBy(memberId, imageKey)) {
            throw new ImageInvalidException();
        }
        FurnitureType furnitureType = furnitureTypeId == null ? null : furnitureTypeRepository.findById(furnitureTypeId)
                .orElseThrow(FurnitureTypeNotFoundException::new);
        Instant pendingSince = ImageAnalysis.pendingSince(Instant.now());
        // 더블탭·재시도로 같은 사진(같은 가구)을 다시 요청하면 진행 중이거나 끝난 분석을 그대로 돌려준다. Gemini를 다시 부르지 않는다
        List<ImageAnalysis> reusable = imageAnalysisRepository.findReusable(memberId, imageKey, furnitureTypeId, pendingSince);
        if (!reusable.isEmpty()) {
            return ImageAnalysisResponse.from(reusable.getFirst());
        }
        // 대기열은 모든 회원이 함께 쓰므로 한 회원이 독점하지 못하게 한다.
        // ponytail: 확인과 저장 사이에 동시 요청이 들어오면 한두 건 넘을 수 있다. 엄격히 막으려면 회원 행을 잠근다
        if (imageAnalysisRepository.countByMemberIdAndStatusAndCreatedAtAfter(
                memberId, ImageAnalysisStatus.PENDING, pendingSince) >= MAX_PENDING_PER_MEMBER) {
            throw new ImageAnalysisTooManyException();
        }
        ThreadPoolTaskExecutor executor = ImageAnalysisWorker.sendsBytes(imageKey)
                ? inlineImageAnalysisExecutor
                : urlImageAnalysisExecutor;
        if (executor.getThreadPoolExecutor().getQueue().remainingCapacity() == 0) {
            throw new ImageAnalysisBusyException();
        }
        ImageAnalysis analysis = imageAnalysisRepository.save(
                ImageAnalysis.request(memberRepository.getReferenceById(memberId), imageKey, furnitureType));
        Long analysisId = analysis.getId();
        // 커밋 전에 작업이 시작되면 워커가 행을 찾지 못한다
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    executor.execute(() -> imageAnalysisWorker.analyze(analysisId));
                } catch (TaskRejectedException e) {
                    // 남은 칸 확인과 투입 사이에 대기열이 찼다. 행은 PENDING으로 남고 60초 뒤 실패로 보인다
                    log.warn("Image analysis {} rejected: queue full", analysisId);
                }
            }
        });
        return ImageAnalysisResponse.from(analysis);
    }

    public ImageAnalysisResponse getAnalysis(Long memberId, Long analysisId) {
        return imageAnalysisRepository.findByIdAndMemberId(analysisId, memberId)
                .map(ImageAnalysisResponse::from)
                .orElseThrow(ImageAnalysisNotFoundException::new);
    }
}

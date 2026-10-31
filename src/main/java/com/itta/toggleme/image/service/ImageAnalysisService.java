package com.itta.toggleme.image.service;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.dto.ImageAnalysisResponse;
import com.itta.toggleme.image.exception.ImageAnalysisBusyException;
import com.itta.toggleme.image.exception.ImageAnalysisNotFoundException;
import com.itta.toggleme.image.exception.ImageInvalidException;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.member.repository.MemberRepository;
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

    private final ImageService imageService;
    private final ImageAnalysisWorker imageAnalysisWorker;
    private final ImageAnalysisRepository imageAnalysisRepository;
    private final MemberRepository memberRepository;
    private final ThreadPoolTaskExecutor urlImageAnalysisExecutor;
    private final ThreadPoolTaskExecutor inlineImageAnalysisExecutor;

    // ponytail: 대기열은 메모리에만 있어 서버가 재시작되면 남은 요청은 60초 뒤 실패로 보인다.
    // 재시작에도 이어서 처리해야 하면 PENDING 행을 주기적으로 다시 꺼내는 방식으로 바꾼다
    @Transactional
    public ImageAnalysisResponse requestAnalysis(Long memberId, String imageKey) {
        if (!imageService.isUploadedBy(memberId, imageKey)) {
            throw new ImageInvalidException();
        }
        ThreadPoolTaskExecutor executor = ImageAnalysisWorker.sendsBytes(imageKey)
                ? inlineImageAnalysisExecutor
                : urlImageAnalysisExecutor;
        if (executor.getThreadPoolExecutor().getQueue().remainingCapacity() == 0) {
            throw new ImageAnalysisBusyException();
        }
        ImageAnalysis analysis = imageAnalysisRepository.save(
                ImageAnalysis.request(memberRepository.getReferenceById(memberId), imageKey));
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

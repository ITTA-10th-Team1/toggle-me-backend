package com.itta.toggleme.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import java.time.Instant;
import java.util.Optional;
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
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.heic");
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.now());
        given(imageAnalysisRepository.findById(1L)).willReturn(Optional.of(analysis));
        given(imageService.download("images/7/a.heic")).willThrow(new IllegalStateException("S3 down"));

        imageAnalysisWorker.analyze(1L);

        assertThat(analysis.getStatus()).isEqualTo(ImageAnalysisStatus.FAILED);
        verify(imageAnalysisRepository).save(analysis);
    }
}

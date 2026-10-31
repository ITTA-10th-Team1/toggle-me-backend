package com.itta.toggleme.image.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.itta.toggleme.image.exception.ImageInvalidException;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class ImageAnalysisServiceTest {

    private final ImageService imageService = mock(ImageService.class);
    private final ImageAnalysisRepository imageAnalysisRepository = mock(ImageAnalysisRepository.class);
    private final ImageAnalysisService imageAnalysisService = new ImageAnalysisService(
            imageService,
            mock(ImageAnalysisWorker.class),
            imageAnalysisRepository,
            mock(MemberRepository.class),
            mock(ThreadPoolTaskExecutor.class),
            mock(ThreadPoolTaskExecutor.class)
    );

    @Test
    void 본인이_올리지_않은_이미지는_분석을_요청할_수_없다() {
        given(imageService.isUploadedBy(7L, "images/8/a.jpg")).willReturn(false);

        assertThatThrownBy(() -> imageAnalysisService.requestAnalysis(7L, "images/8/a.jpg"))
                .isInstanceOf(ImageInvalidException.class);
        verify(imageAnalysisRepository, never()).save(any());
    }
}

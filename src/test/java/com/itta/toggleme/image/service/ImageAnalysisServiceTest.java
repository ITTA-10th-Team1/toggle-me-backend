package com.itta.toggleme.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.dto.ImageAnalysisResponse;
import com.itta.toggleme.image.exception.ImageAnalysisTooManyException;
import com.itta.toggleme.image.exception.ImageInvalidException;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.exception.FurnitureTypeNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class ImageAnalysisServiceTest {

    private static final String IMAGE_KEY = "images/7/a.jpg";

    private final ImageService imageService = mock(ImageService.class);
    private final ImageAnalysisRepository imageAnalysisRepository = mock(ImageAnalysisRepository.class);
    private final FurnitureTypeRepository furnitureTypeRepository = mock(FurnitureTypeRepository.class);
    private final ImageAnalysisService imageAnalysisService = new ImageAnalysisService(
            imageService,
            mock(ImageAnalysisWorker.class),
            imageAnalysisRepository,
            mock(MemberRepository.class),
            furnitureTypeRepository,
            mock(ThreadPoolTaskExecutor.class),
            mock(ThreadPoolTaskExecutor.class)
    );

    @Test
    void 본인이_올리지_않은_이미지는_분석을_요청할_수_없다() {
        given(imageService.isUploadedBy(7L, "images/8/a.jpg")).willReturn(false);

        assertThatThrownBy(() -> imageAnalysisService.requestAnalysis(7L, "images/8/a.jpg", null))
                .isInstanceOf(ImageInvalidException.class);
        verify(imageAnalysisRepository, never()).save(any());
    }

    @Test
    void 없는_가구로는_분석을_요청할_수_없다() {
        given(imageService.isUploadedBy(7L, IMAGE_KEY)).willReturn(true);
        given(furnitureTypeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> imageAnalysisService.requestAnalysis(7L, IMAGE_KEY, 99L))
                .isInstanceOf(FurnitureTypeNotFoundException.class);
        verify(imageAnalysisRepository, never()).save(any());
    }

    @Test
    void 같은_사진을_다시_요청하면_진행_중이거나_끝난_분석을_돌려준다() {
        ImageAnalysis existing = ImageAnalysis.request(null, IMAGE_KEY, null);
        ReflectionTestUtils.setField(existing, "id", 5L);
        ReflectionTestUtils.setField(existing, "createdAt", Instant.now());
        given(imageService.isUploadedBy(7L, IMAGE_KEY)).willReturn(true);
        given(imageAnalysisRepository.findReusable(eq(7L), eq(IMAGE_KEY), isNull(), any())).willReturn(List.of(existing));

        ImageAnalysisResponse response = imageAnalysisService.requestAnalysis(7L, IMAGE_KEY, null);

        assertThat(response.analysisId()).isEqualTo(5L);
        verify(imageAnalysisRepository, never()).save(any());
    }

    @Test
    void 진행_중인_분석이_3건이면_더_요청할_수_없다() {
        given(imageService.isUploadedBy(7L, IMAGE_KEY)).willReturn(true);
        given(imageAnalysisRepository.findReusable(eq(7L), eq(IMAGE_KEY), isNull(), any())).willReturn(List.of());
        given(imageAnalysisRepository.countByMemberIdAndStatusAndCreatedAtAfter(
                eq(7L), eq(ImageAnalysisStatus.PENDING), any())).willReturn(3L);

        assertThatThrownBy(() -> imageAnalysisService.requestAnalysis(7L, IMAGE_KEY, null))
                .isInstanceOf(ImageAnalysisTooManyException.class);
        verify(imageAnalysisRepository, never()).save(any());
    }
}

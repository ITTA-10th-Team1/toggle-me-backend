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
import java.time.Instant;
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
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.heic");
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.now());
        given(imageAnalysisRepository.findById(1L)).willReturn(Optional.of(analysis));
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
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.jpg");
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.now());
        given(imageAnalysisRepository.findById(1L)).willReturn(Optional.of(analysis));
        given(imageService.createViewUrl("images/7/a.jpg")).willReturn("https://view/images/7/a.jpg");

        worker.analyze(1L);

        assertThat(analysis.getStatus()).isEqualTo(ImageAnalysisStatus.FAILED);
        assertThat(analysis.getResult()).isNull();
    }
}


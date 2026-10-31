package com.itta.toggleme.image.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ImageAnalysisTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-31T00:00:00Z");

    @Test
    void 대기_중인_분석은_60초가_지나면_실패로_보인다() {
        ImageAnalysis analysis = requestedAt(CREATED_AT);

        assertThat(analysis.currentStatus(CREATED_AT.plusSeconds(60))).isEqualTo(ImageAnalysisStatus.PENDING);
        assertThat(analysis.currentStatus(CREATED_AT.plusSeconds(61))).isEqualTo(ImageAnalysisStatus.FAILED);
    }

    @Test
    void 완료된_분석은_시간이_지나도_완료다() {
        ImageAnalysis analysis = requestedAt(CREATED_AT);
        analysis.complete(new ImageAnalysisResult(List.of("#추리")));

        assertThat(analysis.currentStatus(CREATED_AT.plusSeconds(3600))).isEqualTo(ImageAnalysisStatus.COMPLETED);
    }

    private ImageAnalysis requestedAt(Instant createdAt) {
        ImageAnalysis analysis = ImageAnalysis.request(null, "images/7/a.jpg");
        ReflectionTestUtils.setField(analysis, "createdAt", createdAt);
        return analysis;
    }
}

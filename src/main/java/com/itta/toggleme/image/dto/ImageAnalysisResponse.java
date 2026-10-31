package com.itta.toggleme.image.dto;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisResult;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import java.time.Instant;

// result는 status가 COMPLETED일 때만 채워진다
public record ImageAnalysisResponse(
        Long analysisId,
        ImageAnalysisStatus status,
        ImageAnalysisResult result
) {

    public static ImageAnalysisResponse from(ImageAnalysis analysis) {
        ImageAnalysisStatus status = analysis.getCreatedAt() == null
                ? analysis.getStatus()
                : analysis.currentStatus(Instant.now());
        return new ImageAnalysisResponse(
                analysis.getId(),
                status,
                status == ImageAnalysisStatus.COMPLETED ? analysis.getResult() : null
        );
    }
}

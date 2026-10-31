package com.itta.toggleme.image.dto;

import jakarta.validation.constraints.NotBlank;

public record ImageAnalysisCreateRequest(
        @NotBlank(message = "이미지 키는 필수입니다.")
        String imageKey
) {
}

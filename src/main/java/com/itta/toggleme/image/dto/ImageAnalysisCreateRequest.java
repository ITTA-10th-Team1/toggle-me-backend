package com.itta.toggleme.image.dto;

import jakarta.validation.constraints.NotBlank;

public record ImageAnalysisCreateRequest(
        @NotBlank(message = "이미지 키는 필수입니다.")
        String imageKey,

        // 어느 가구에 기록할 사진인지. 보내면 가구에 맞는 태그를 뽑고, 비우면 일반 규칙으로 뽑는다
        Long furnitureTypeId
) {
}

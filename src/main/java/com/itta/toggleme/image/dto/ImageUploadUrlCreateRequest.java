package com.itta.toggleme.image.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ImageUploadUrlCreateRequest(
        @NotNull(message = "이미지 형식은 필수입니다.")
        @Pattern(regexp = "image/(jpeg|png|heic)", message = "jpeg, png, heic 이미지만 올릴 수 있습니다.")
        String contentType,

        @NotNull(message = "이미지 크기는 필수입니다.")
        @Positive(message = "이미지 크기는 0보다 커야 합니다.")
        @Max(value = 10 * 1024 * 1024, message = "이미지는 10MB 이하만 올릴 수 있습니다.")
        Long contentLength
) {
}

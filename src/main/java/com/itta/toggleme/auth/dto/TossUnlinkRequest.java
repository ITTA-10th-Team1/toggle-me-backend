package com.itta.toggleme.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TossUnlinkRequest(
        @NotBlank(message = "userKey는 필수입니다.")
        String userKey,

        String referrer
) {
}

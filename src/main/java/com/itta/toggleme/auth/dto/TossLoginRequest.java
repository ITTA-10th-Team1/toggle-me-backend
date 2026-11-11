package com.itta.toggleme.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TossLoginRequest(
        @NotBlank(message = "토스 인가 코드는 필수입니다.")
        @Size(max = 2000, message = "토스 인가 코드가 너무 깁니다.")
        String authorizationCode,

        @NotBlank(message = "referrer는 필수입니다.")
        @Size(max = 20, message = "referrer가 너무 깁니다.")
        String referrer
) {

    // 인가 코드는 한 번 쓰면 끝나는 비밀 값이라 로그에 남기지 않는다
    @Override
    public String toString() {
        return "TossLoginRequest[referrer=" + referrer + "]";
    }
}

package com.itta.toggleme.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 가입 전 아이디 중복확인 쿼리 파라미터. 규칙은 가입 요청({@link LocalSignUpRequest})과 같다. */
public record LoginIdAvailabilityRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "[a-z0-9]{4,20}",
                message = "아이디는 영문 소문자와 숫자만 사용해 4~20자로 입력해 주세요.")
        String loginId
) {
}

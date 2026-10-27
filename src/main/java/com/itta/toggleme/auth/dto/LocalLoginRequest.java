package com.itta.toggleme.auth.dto;

import com.itta.toggleme.global.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalLoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @MaxBytes(value = 72, message = "비밀번호가 너무 깁니다. 영문 72자 또는 한글 24자 이하로 입력해 주세요.")
        String password
) {

    @Override
    public String toString() {
        return "LocalLoginRequest[email=" + email + "]";
    }
}

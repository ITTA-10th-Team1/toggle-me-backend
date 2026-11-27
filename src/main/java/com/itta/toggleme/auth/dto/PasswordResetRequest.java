package com.itta.toggleme.auth.dto;

import com.itta.toggleme.global.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotBlank(message = "인증 코드는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "인증 코드는 숫자 6자리입니다.")
        String code,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        @MaxBytes(value = 72, message = "비밀번호가 너무 깁니다. 영문 72자 또는 한글 24자 이하로 입력해 주세요.")
        String newPassword
) {

    @Override
    public String toString() {
        return "PasswordResetRequest[email=" + email + "]";
    }
}

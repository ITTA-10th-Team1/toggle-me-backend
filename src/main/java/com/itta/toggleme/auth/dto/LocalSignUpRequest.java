package com.itta.toggleme.auth.dto;

import com.itta.toggleme.global.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocalSignUpRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "[a-z0-9]{4,20}",
                message = "아이디는 영문 소문자와 숫자만 사용해 4~20자로 입력해 주세요.")
        String loginId,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        @MaxBytes(value = 72, message = "비밀번호가 너무 깁니다. 영문 72자 또는 한글 24자 이하로 입력해 주세요.")
        String password,

        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname
) {

    @Override
    public String toString() {
        return "LocalSignUpRequest[loginId=" + loginId + ", email=" + email + ", nickname=" + nickname + "]";
    }
}

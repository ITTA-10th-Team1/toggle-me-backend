package com.itta.toggleme.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
        @NotBlank(message = "핸들은 필수입니다.") String handle,
        @NotBlank(message = "닉네임은 필수입니다.") @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.") String nickname
) { }

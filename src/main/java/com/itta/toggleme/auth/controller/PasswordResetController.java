package com.itta.toggleme.auth.controller;

import com.itta.toggleme.auth.dto.PasswordResetCodeRequest;
import com.itta.toggleme.auth.dto.PasswordResetRequest;
import com.itta.toggleme.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(summary = "비밀번호 재설정 코드 발송 (가입된 자체 계정 이메일이면 메일 발송, 아니어도 204로 같은 응답)")
    @PostMapping("/codes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        passwordResetService.requestCode(request.email());
    }

    @Operation(summary = "비밀번호 재설정 (코드 확인 후 새 비밀번호로 변경, 모든 기기 로그아웃)")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
    }
}

package com.itta.toggleme.auth.controller;

import com.itta.toggleme.auth.dto.EmailVerificationConfirmRequest;
import com.itta.toggleme.auth.dto.EmailVerificationSendRequest;
import com.itta.toggleme.auth.dto.EmailVerificationTokenResponse;
import com.itta.toggleme.auth.service.EmailVerificationService;
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
@RequestMapping("/api/v1/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "가입용 이메일 인증 코드 발송 (6자리, 10분 유효, 60초 간격·시간당 5회 제한)")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void send(@Valid @RequestBody EmailVerificationSendRequest request) {
        emailVerificationService.requestCode(request.email());
    }

    @Operation(summary = "이메일 인증 코드 확인 (성공하면 가입에 쓰는 verificationToken 발급, 30분 유효·1회용)")
    @PostMapping("/confirm")
    public EmailVerificationTokenResponse confirm(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        return new EmailVerificationTokenResponse(
                emailVerificationService.confirmCode(request.email(), request.code()));
    }
}

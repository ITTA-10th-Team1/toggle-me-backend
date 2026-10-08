package com.itta.toggleme.auth.controller;

import com.itta.toggleme.auth.dto.KakaoLoginRequest;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/oauth/kakao")
    public ResponseEntity<LoginResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithKakao(request.accessToken()));
    }
}

package com.itta.toggleme.auth.controller;

import com.itta.toggleme.auth.dto.KakaoLoginRequest;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.dto.RefreshTokenRequest;
import com.itta.toggleme.auth.dto.TokenResponse;
import com.itta.toggleme.auth.service.AuthService;
import com.itta.toggleme.auth.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/oauth/kakao")
    public ResponseEntity<LoginResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithKakao(request.accessToken()));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(tokenService.reissue(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        tokenService.revoke(Long.valueOf(jwt.getSubject()), request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}

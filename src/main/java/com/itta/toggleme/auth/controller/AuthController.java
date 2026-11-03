package com.itta.toggleme.auth.controller;

import com.itta.toggleme.auth.dto.KakaoLoginRequest;
import com.itta.toggleme.auth.dto.LocalLoginRequest;
import com.itta.toggleme.auth.dto.LocalSignUpRequest;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.dto.RefreshTokenRequest;
import com.itta.toggleme.auth.dto.TokenResponse;
import com.itta.toggleme.auth.service.AuthService;
import com.itta.toggleme.auth.service.LocalAuthService;
import com.itta.toggleme.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "카카오·자체 계정 로그인, 토큰 갱신, 로그아웃")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final LocalAuthService localAuthService;
    private final TokenService tokenService;

    @Operation(summary = "카카오 로그인·회원가입 (첫 로그인이면 자동 가입)")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<LoginResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithKakao(request.accessToken()));
    }

    @Operation(summary = "자체 회원가입 (가입 즉시 토큰 발급)")
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signUp(@Valid @RequestBody LocalSignUpRequest request) {
        LoginResponse response = localAuthService.signUp(
                request.loginId(), request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "자체 로그인 (아이디·비밀번호)")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LocalLoginRequest request) {
        return ResponseEntity.ok(localAuthService.login(request.loginId(), request.password()));
    }

    @Operation(summary = "토큰 갱신 (Refresh Token은 1회용)")
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(tokenService.reissue(request.refreshToken()));
    }

    @Operation(summary = "로그아웃 (Refresh Token 폐기)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        tokenService.revoke(Long.valueOf(jwt.getSubject()), request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}

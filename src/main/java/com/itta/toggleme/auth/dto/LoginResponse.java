package com.itta.toggleme.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean onboardingRequired
) {

    public static LoginResponse of(TokenResponse token, boolean onboardingRequired) {
        return new LoginResponse(
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.expiresIn(),
                onboardingRequired
        );
    }
}

package com.itta.toggleme.auth.dto;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.OnboardingStep;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean onboardingRequired,
        OnboardingStep onboardingStep
) {

    public static LoginResponse of(TokenResponse token, Member member) {
        OnboardingStep step = member.getOnboardingStep();
        return new LoginResponse(
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.expiresIn(),
                step != OnboardingStep.DONE,
                step
        );
    }
}

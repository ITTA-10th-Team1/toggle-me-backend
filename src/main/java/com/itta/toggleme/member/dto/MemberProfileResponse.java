package com.itta.toggleme.member.dto;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.OnboardingStep;

public record MemberProfileResponse(
        Long memberId,
        String handle,
        String nickname,
        boolean profileCompleted,
        OnboardingStep onboardingStep
) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getHandle(),
                member.getNickname(),
                member.isProfileCompleted(),
                member.getOnboardingStep()
        );
    }
}

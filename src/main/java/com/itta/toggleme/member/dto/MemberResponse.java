package com.itta.toggleme.member.dto;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.OnboardingStep;

public record MemberResponse(
        Long memberId,
        String handle,
        String nickname,
        boolean profileCompleted,
        OnboardingStep onboardingStep
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getHandle(),
                member.getNickname(),
                member.isProfileCompleted(),
                member.getOnboardingStep()
        );
    }
}

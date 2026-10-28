package com.itta.toggleme.member.dto;

import com.itta.toggleme.member.domain.Member;

public record MemberProfileResponse(Long memberId, String handle, String nickname, boolean profileCompleted) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getHandle(),
                member.getNickname(),
                member.isProfileCompleted()
        );
    }
}

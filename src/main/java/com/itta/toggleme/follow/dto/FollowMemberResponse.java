package com.itta.toggleme.follow.dto;

import com.itta.toggleme.member.domain.Member;

// 팔로워·팔로잉 목록의 한 줄
public record FollowMemberResponse(
        Long memberId,
        String handle,
        String nickname
) {
    public static FollowMemberResponse from(Member member) {
        return new FollowMemberResponse(member.getId(), member.getHandle(), member.getNickname());
    }
}

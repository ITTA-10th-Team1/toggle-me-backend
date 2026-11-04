package com.itta.toggleme.follow.dto;

// 검색 결과 한 줄. following: 내가 이미 팔로우 중인지
public record MemberSearchResponse(
        Long memberId,
        String handle,
        String nickname,
        boolean following
) {
}

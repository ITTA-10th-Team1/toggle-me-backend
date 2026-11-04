package com.itta.toggleme.follow.dto;

/**
 * 다른 회원(또는 나)의 프로필. 방이 OFF인 회원은 본인에게만 보인다.
 *
 * @param following  내가 이 회원을 팔로우 중인지. 내 프로필이면 false
 * @param roomPublic 방이 ON인지. 다른 회원 프로필은 항상 true(OFF면 404), 내 프로필에서만 false가 나온다
 */
public record MemberProfileResponse(
        Long memberId,
        String handle,
        String nickname,
        long followerCount,
        long followingCount,
        boolean following,
        boolean roomPublic
) {
}

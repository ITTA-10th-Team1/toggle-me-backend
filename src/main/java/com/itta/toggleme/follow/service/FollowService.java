package com.itta.toggleme.follow.service;

import com.itta.toggleme.follow.dto.FollowMemberResponse;
import com.itta.toggleme.follow.dto.MemberProfileResponse;
import com.itta.toggleme.follow.exception.SelfFollowException;
import com.itta.toggleme.follow.repository.FollowRepository;
import com.itta.toggleme.member.domain.Member;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단방향 팔로우. 승인 없이 바로 맺는다.
 * 방이 OFF인 회원은 다른 사람에게 없는 회원처럼 보인다(PRD ROOM-33, 판단은 VisibleMemberService): 프로필·팔로우는 404, 목록·수에서 빠진다.
 * 팔로우 행은 지우지 않아 다시 ON이면 그대로 돌아온다(ROOM-34). 본인은 OFF여도 다른 사람을 보고 팔로우할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final VisibleMemberService visibleMemberService;

    @Transactional
    public void follow(Long memberId, String handle) {
        Member target = visibleMemberService.find(memberId, handle);
        if (target.getId().equals(memberId)) {
            throw new SelfFollowException();
        }
        followRepository.insertIfAbsent(memberId, target.getId());
    }

    @Transactional
    public void unfollow(Long memberId, String handle) {
        followRepository.deleteByFollowerIdAndFolloweeId(memberId, visibleMemberService.find(memberId, handle).getId());
    }

    public MemberProfileResponse getProfile(Long memberId, String handle) {
        Member target = visibleMemberService.find(memberId, handle);
        Long targetId = target.getId();
        return new MemberProfileResponse(
                targetId,
                target.getHandle(),
                target.getNickname(),
                followRepository.countFollowers(targetId),
                followRepository.countFollowing(targetId),
                followRepository.existsByFollowerIdAndFolloweeId(memberId, targetId),
                visibleMemberService.isRoomPublic(targetId)
        );
    }

    public List<FollowMemberResponse> getFollowers(Long memberId, String handle) {
        return followRepository.findFollowers(visibleMemberService.find(memberId, handle).getId()).stream()
                .map(FollowMemberResponse::from)
                .toList();
    }

    public List<FollowMemberResponse> getFollowing(Long memberId, String handle) {
        return followRepository.findFollowing(visibleMemberService.find(memberId, handle).getId()).stream()
                .map(FollowMemberResponse::from)
                .toList();
    }
}

package com.itta.toggleme.follow.service;

import com.itta.toggleme.follow.dto.FollowMemberResponse;
import com.itta.toggleme.follow.dto.MemberProfileResponse;
import com.itta.toggleme.follow.exception.SelfFollowException;
import com.itta.toggleme.follow.repository.FollowRepository;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.exception.MemberNotFoundException;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.RoomStatus;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.RoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단방향 팔로우. 승인 없이 바로 맺는다.
 * 방이 OFF인 회원은 다른 사람에게 없는 회원처럼 보인다(PRD ROOM-33): 프로필·팔로우는 404, 목록·수에서 빠진다.
 * 팔로우 행은 지우지 않아 다시 ON이면 그대로 돌아온다(ROOM-34). 본인은 OFF여도 다른 사람을 보고 팔로우할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public void follow(Long memberId, String handle) {
        Member target = findVisible(memberId, handle);
        if (target.getId().equals(memberId)) {
            throw new SelfFollowException();
        }
        followRepository.insertIfAbsent(memberId, target.getId());
    }

    @Transactional
    public void unfollow(Long memberId, String handle) {
        followRepository.deleteByFollowerIdAndFolloweeId(memberId, findVisible(memberId, handle).getId());
    }

    public MemberProfileResponse getProfile(Long memberId, String handle) {
        Member target = findVisible(memberId, handle);
        Long targetId = target.getId();
        return new MemberProfileResponse(
                targetId,
                target.getHandle(),
                target.getNickname(),
                followRepository.countFollowers(targetId),
                followRepository.countFollowing(targetId),
                followRepository.existsByFollowerIdAndFolloweeId(memberId, targetId),
                isRoomPublic(targetId)
        );
    }

    public List<FollowMemberResponse> getFollowers(Long memberId, String handle) {
        return followRepository.findFollowers(findVisible(memberId, handle).getId()).stream()
                .map(FollowMemberResponse::from)
                .toList();
    }

    public List<FollowMemberResponse> getFollowing(Long memberId, String handle) {
        return followRepository.findFollowing(findVisible(memberId, handle).getId()).stream()
                .map(FollowMemberResponse::from)
                .toList();
    }

    // 다른 사람이 보는 회원: 탈퇴하지 않았고 방이 ON. 본인은 방이 OFF여도 보인다
    private Member findVisible(Long viewerId, String handle) {
        Member member = memberRepository.findByHandleAndStatus(handle, MemberStatus.ACTIVE)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.getId().equals(viewerId) && !isRoomPublic(member.getId())) {
            throw new MemberNotFoundException();
        }
        return member;
    }

    private boolean isRoomPublic(Long memberId) {
        return roomRepository.findFirstByOwnerIdOrderById(memberId)
                .filter(room -> room.getStatus() == RoomStatus.ACTIVE)
                .filter(room -> room.getVisibility() == RoomVisibility.SOCIAL_ON)
                .isPresent();
    }
}

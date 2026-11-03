package com.itta.toggleme.follow.service;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.exception.MemberNotFoundException;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.RoomStatus;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.RoomRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 사람에게 보이는 회원인지 판단한다(PRD ROOM-33). 팔로우·남의 방 보기가 같이 쓴다.
 * 탈퇴했거나 방이 OFF·없는 회원은 없는 회원처럼 404. 본인은 방이 OFF여도 보인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisibleMemberService {

    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;

    // 고유 ID는 소문자로 저장되므로 주소에 대문자가 섞여도 찾는다
    public Member find(Long viewerId, String handle) {
        Member member = memberRepository.findByHandleAndStatus(handle.strip().toLowerCase(Locale.ROOT), MemberStatus.ACTIVE)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.getId().equals(viewerId) && !isRoomPublic(member.getId())) {
            throw new MemberNotFoundException();
        }
        return member;
    }

    public boolean isRoomPublic(Long memberId) {
        return roomRepository.findFirstByOwnerIdOrderById(memberId)
                .filter(room -> room.getStatus() == RoomStatus.ACTIVE)
                .filter(room -> room.getVisibility() == RoomVisibility.SOCIAL_ON)
                .isPresent();
    }
}

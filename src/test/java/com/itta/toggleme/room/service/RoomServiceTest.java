package com.itta.toggleme.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.RoomFloor;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.exception.RoomStyleNotFoundException;
import com.itta.toggleme.room.repository.RoomFloorRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomFloorRepository roomFloorRepository;

    @Autowired
    private RoomStyleRepository roomStyleRepository;

    @Test
    void 공개_설정을_지정해_방과_1층을_생성한다() {
        saveDefaultStyles();
        Member owner = saveMember();

        Long roomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF);

        assertThat(roomRepository.findById(roomId)).hasValueSatisfying(room -> {
            assertThat(room.getName()).isEqualTo("나의 방");
            assertThat(room.getVisibility()).isEqualTo(RoomVisibility.SOCIAL_OFF);
        });
        List<RoomFloor> floors = roomFloorRepository.findAllByRoomIdOrderByFloorNumber(roomId);
        assertThat(floors).singleElement().satisfies(floor -> {
            assertThat(floor.getFloorNumber()).isEqualTo(1);
            assertThat(floor.getName()).isEqualTo("1층");
        });
    }

    @Test
    void 공개_방을_생성한다() {
        saveDefaultStyles();
        Member owner = saveMember();

        Long roomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_ON);

        assertThat(roomRepository.findById(roomId)).hasValueSatisfying(room ->
                assertThat(room.getVisibility()).isEqualTo(RoomVisibility.SOCIAL_ON));
    }

    @Test
    void 이미_방이_있으면_다른_공개_설정으로_재요청해도_기존_방을_유지한다() {
        saveDefaultStyles();
        Member owner = saveMember();
        Long firstRoomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF);

        Long secondRoomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_ON);

        assertThat(secondRoomId).isEqualTo(firstRoomId);
        assertThat(roomRepository.findAllByOwnerIdOrderById(owner.getId())).singleElement().satisfies(room ->
                assertThat(room.getVisibility()).isEqualTo(RoomVisibility.SOCIAL_OFF));
    }

    @Test
    void 기본_스타일이_없으면_방을_만들_수_없다() {
        Member owner = saveMember();

        assertThatThrownBy(() ->
                roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF))
                .isInstanceOf(RoomStyleNotFoundException.class);
    }

    private void saveDefaultStyles() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
    }

    private Member saveMember() {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(Member.create(handle, "토글러"));
    }
}

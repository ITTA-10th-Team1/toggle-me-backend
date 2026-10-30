package com.itta.toggleme.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.member.domain.Member;
import org.junit.jupiter.api.Test;

class RoomFloorTest {

    @Test
    void 벽과_바닥_스타일로_층을_생성한다() {
        Room room = Room.create(Member.create("user_room", "토글러"), "나의 방", RoomVisibility.SOCIAL_OFF);
        RoomStyle wallStyle = RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb");
        RoomStyle floorStyle = RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb");

        RoomFloor floor = RoomFloor.create(room, wallStyle, floorStyle, 1, "1층");

        assertThat(floor.getRoom()).isEqualTo(room);
        assertThat(floor.getWallStyle()).isEqualTo(wallStyle);
        assertThat(floor.getFloorStyle()).isEqualTo(floorStyle);
        assertThat(floor.getFloorNumber()).isEqualTo(1);
    }

    @Test
    void 벽과_바닥_스타일_유형이_바뀌면_층을_생성할_수_없다() {
        Room room = Room.create(Member.create("user_room", "토글러"), "나의 방", RoomVisibility.SOCIAL_OFF);
        RoomStyle wallStyle = RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb");
        RoomStyle floorStyle = RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb");

        assertThatThrownBy(() -> RoomFloor.create(room, floorStyle, wallStyle, 1, "1층"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

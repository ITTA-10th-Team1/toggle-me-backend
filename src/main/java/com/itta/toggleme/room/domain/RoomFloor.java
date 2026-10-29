package com.itta.toggleme.room.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomFloor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wall_style_id", nullable = false)
    private RoomStyle wallStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_style_id", nullable = false)
    private RoomStyle floorStyle;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(nullable = false, length = 50)
    private String name;

    private RoomFloor(
            Room room,
            RoomStyle wallStyle,
            RoomStyle floorStyle,
            int floorNumber,
            String name
    ) {
        if (wallStyle.getType() != RoomStyleType.WALL || floorStyle.getType() != RoomStyleType.FLOOR) {
            throw new IllegalArgumentException("방 스타일 유형이 올바르지 않습니다.");
        }
        this.room = room;
        this.wallStyle = wallStyle;
        this.floorStyle = floorStyle;
        this.floorNumber = floorNumber;
        this.name = name;
    }

    public static RoomFloor create(
            Room room,
            RoomStyle wallStyle,
            RoomStyle floorStyle,
            int floorNumber,
            String name
    ) {
        return new RoomFloor(room, wallStyle, floorStyle, floorNumber, name);
    }
}

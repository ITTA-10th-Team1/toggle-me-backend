package com.itta.toggleme.room.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import com.itta.toggleme.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    private Room(Member owner, String name, RoomVisibility visibility) {
        this.owner = owner;
        this.name = name;
        this.visibility = visibility;
        this.status = RoomStatus.ACTIVE;
    }

    public static Room create(Member owner, String name, RoomVisibility visibility) {
        return new Room(owner, name, visibility);
    }

    public void changeVisibility(RoomVisibility visibility) {
        this.visibility = visibility;
    }
}

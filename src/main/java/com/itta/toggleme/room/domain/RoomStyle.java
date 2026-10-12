package com.itta.toggleme.room.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomStyle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStyleType type;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "asset_object_key", nullable = false, length = 500)
    private String assetObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status;

    private RoomStyle(RoomStyleType type, String name, String assetObjectKey) {
        this.type = type;
        this.name = name;
        this.assetObjectKey = assetObjectKey;
        this.status = CatalogStatus.ACTIVE;
    }

    public static RoomStyle create(RoomStyleType type, String name, String assetObjectKey) {
        return new RoomStyle(type, name, assetObjectKey);
    }
}

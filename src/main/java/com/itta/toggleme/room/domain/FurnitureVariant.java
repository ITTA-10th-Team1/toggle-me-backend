package com.itta.toggleme.room.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
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
public class FurnitureVariant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "furniture_type_id", nullable = false)
    private FurnitureType furnitureType;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "asset_object_key", nullable = false, length = 500)
    private String assetObjectKey;

    @Column(name = "width_units", nullable = false)
    private int widthUnits;

    @Column(name = "height_units", nullable = false)
    private int heightUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status;

    private FurnitureVariant(
            FurnitureType furnitureType,
            String name,
            String assetObjectKey,
            int widthUnits,
            int heightUnits
    ) {
        this.furnitureType = furnitureType;
        this.name = name;
        this.assetObjectKey = assetObjectKey;
        this.widthUnits = widthUnits;
        this.heightUnits = heightUnits;
        this.status = CatalogStatus.ACTIVE;
    }

    public static FurnitureVariant create(
            FurnitureType furnitureType,
            String name,
            String assetObjectKey,
            int widthUnits,
            int heightUnits
    ) {
        return new FurnitureVariant(furnitureType, name, assetObjectKey, widthUnits, heightUnits);
    }
}

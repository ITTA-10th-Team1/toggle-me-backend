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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomFurniture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private RoomFloor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private FurnitureVariant variant;

    @Column(name = "position_x", nullable = false, precision = 10, scale = 4)
    private BigDecimal positionX;

    @Column(name = "position_y", nullable = false, precision = 10, scale = 4)
    private BigDecimal positionY;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rotation;

    @Column(name = "render_order", nullable = false)
    private int renderOrder;

    @Column(nullable = false)
    private boolean active;

    private RoomFurniture(
            RoomFloor floor,
            FurnitureVariant variant,
            BigDecimal positionX,
            BigDecimal positionY,
            BigDecimal rotation,
            int renderOrder
    ) {
        this.floor = floor;
        this.variant = variant;
        this.positionX = positionX;
        this.positionY = positionY;
        this.rotation = rotation;
        this.renderOrder = renderOrder;
        this.active = true;
    }

    public static RoomFurniture place(
            RoomFloor floor,
            FurnitureVariant variant,
            BigDecimal positionX,
            BigDecimal positionY,
            BigDecimal rotation,
            int renderOrder
    ) {
        return new RoomFurniture(floor, variant, positionX, positionY, rotation, renderOrder);
    }

    public void customize(FurnitureVariant variant, boolean hidden) {
        if (!this.variant.getFurnitureType().getId().equals(variant.getFurnitureType().getId())) {
            throw new IllegalArgumentException("같은 종류의 가구 디자인만 선택할 수 있습니다.");
        }
        this.variant = variant;
        this.active = !hidden;
    }
}

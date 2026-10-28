package com.itta.toggleme.room.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FurnitureType extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "position_x", precision = 10, scale = 4)
    private BigDecimal positionX;

    @Column(name = "position_y", precision = 10, scale = 4)
    private BigDecimal positionY;

    @Column(precision = 10, scale = 4)
    private BigDecimal rotation;

    @Column(name = "render_order")
    private Integer renderOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status;

    private FurnitureType(String code, String name, int displayOrder) {
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
        this.status = CatalogStatus.ACTIVE;
    }

    public static FurnitureType create(String code, String name, int displayOrder) {
        return new FurnitureType(code, name, displayOrder);
    }

    public void configurePlacement(
            BigDecimal positionX,
            BigDecimal positionY,
            BigDecimal rotation,
            int renderOrder
    ) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.rotation = rotation;
        this.renderOrder = renderOrder;
    }

    public boolean hasConfiguredPlacement() {
        return positionX != null && positionY != null && rotation != null && renderOrder != null;
    }
}

package com.itta.toggleme.room.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

    // 방을 만들 때 room_furniture로 복사하는 기본 배치. 아직 정하지 않았으면 null
    @Embedded
    private Placement placement;

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

    public void configurePlacement(Placement placement, int renderOrder) {
        this.placement = placement;
        this.renderOrder = renderOrder;
    }

    public boolean hasConfiguredPlacement() {
        return placement != null && placement.isComplete() && renderOrder != null;
    }
}

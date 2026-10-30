package com.itta.toggleme.item.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.room.domain.FurnitureType;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "furniture_type_id", nullable = false)
    private FurnitureType furnitureType;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    @Column(name = "image_key", nullable = false, unique = true, length = 500)
    private String imageKey;

    @Column(length = 200)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemVisibility visibility;

    private Item(
            Member member,
            FurnitureType furnitureType,
            LocalDate recordedOn,
            String imageKey,
            String memo,
            ItemVisibility visibility
    ) {
        this.member = member;
        this.furnitureType = furnitureType;
        this.recordedOn = recordedOn;
        this.imageKey = imageKey;
        this.memo = memo;
        this.visibility = visibility;
    }

    public static Item record(
            Member member,
            FurnitureType furnitureType,
            LocalDate recordedOn,
            String imageKey,
            String memo,
            ItemVisibility visibility
    ) {
        return new Item(member, furnitureType, recordedOn, imageKey, memo, visibility);
    }

    public void update(String memo, ItemVisibility visibility) {
        this.memo = memo;
        this.visibility = visibility;
    }
}

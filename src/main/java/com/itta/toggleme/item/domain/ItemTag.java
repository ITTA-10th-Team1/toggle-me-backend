package com.itta.toggleme.item.domain;

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

// 아이템에 붙인 태그. 방 태그는 이 행을 가구별로 세어 만든다
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // '#' 없이 저장한다. 보여줄 때 앱이 붙인다
    @Column(nullable = false, length = 20)
    private String name;

    // 사용자가 고른 순서(0부터). 방 태그를 고를 때 마지막 동점 기준이다
    @Column(nullable = false)
    private int position;

    private ItemTag(Item item, String name, int position) {
        this.item = item;
        this.name = name;
        this.position = position;
    }

    public static ItemTag of(Item item, String name, int position) {
        return new ItemTag(item, name, position);
    }
}

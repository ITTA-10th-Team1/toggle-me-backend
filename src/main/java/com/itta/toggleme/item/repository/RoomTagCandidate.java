package com.itta.toggleme.item.repository;

import java.time.Instant;

// 방 태그 계산에 쓰는 태그 한 줄: 어느 가구의 어느 아이템에 붙은 몇 번째 태그인지
public record RoomTagCandidate(
        Long furnitureTypeId,
        String name,
        int position,
        Long itemId,
        Instant itemCreatedAt
) {
}

package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemVisibility;
import java.time.Instant;
import java.time.LocalDate;

// imageUrl은 일정 시간 뒤 만료되는 보기 주소다. 만료되면 목록을 다시 조회한다
public record FridgeFoodResponse(
        Long id,
        LocalDate recordedOn,
        String imageUrl,
        String memo,
        ItemVisibility visibility,
        Instant createdAt
) {

    public static FridgeFoodResponse from(Item item, String imageUrl) {
        return new FridgeFoodResponse(
                item.getId(),
                item.getRecordedOn(),
                imageUrl,
                item.getMemo(),
                item.getVisibility(),
                item.getCreatedAt()
        );
    }
}

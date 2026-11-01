package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemVisibility;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ItemResponse(
        Long id,
        Long furnitureTypeId,
        LocalDate recordedOn,
        String imageUrl,
        String memo,
        ItemVisibility visibility,
        // '#' 없는 태그 이름, 고른 순서대로
        List<String> tags,
        Instant createdAt
) {

    public static ItemResponse from(Item item, String imageUrl, List<String> tags) {
        return new ItemResponse(
                item.getId(),
                item.getFurnitureType().getId(),
                item.getRecordedOn(),
                imageUrl,
                item.getMemo(),
                item.getVisibility(),
                tags,
                item.getCreatedAt()
        );
    }
}

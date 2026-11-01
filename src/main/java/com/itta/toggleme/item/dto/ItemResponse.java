package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemVisibility;
import java.time.Instant;
import java.time.LocalDate;

public record ItemResponse(
        Long id,
        Long furnitureTypeId,
        LocalDate recordedOn,
        String imageUrl,
        String memo,
        ItemVisibility visibility,
        Instant createdAt
) {

    public static ItemResponse from(Item item, String imageUrl) {
        return new ItemResponse(
                item.getId(),
                item.getFurnitureType().getId(),
                item.getRecordedOn(),
                imageUrl,
                item.getMemo(),
                item.getVisibility(),
                item.getCreatedAt()
        );
    }
}

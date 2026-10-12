package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.FurnitureType;

public record FurnitureTypeResponse(
        Long id,
        String code,
        String name,
        int displayOrder
) {

    public static FurnitureTypeResponse from(FurnitureType type) {
        return new FurnitureTypeResponse(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getDisplayOrder()
        );
    }
}

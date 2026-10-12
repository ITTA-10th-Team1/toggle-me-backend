package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.FurnitureVariant;

public record FurnitureVariantResponse(
        Long id,
        Long furnitureTypeId,
        String name,
        String assetObjectKey,
        int widthUnits,
        int heightUnits
) {

    public static FurnitureVariantResponse from(FurnitureVariant variant) {
        return new FurnitureVariantResponse(
                variant.getId(),
                variant.getFurnitureType().getId(),
                variant.getName(),
                variant.getAssetObjectKey(),
                variant.getWidthUnits(),
                variant.getHeightUnits()
        );
    }
}

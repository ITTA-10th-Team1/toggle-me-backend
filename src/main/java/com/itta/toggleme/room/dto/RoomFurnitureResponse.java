package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.RoomFurniture;
import java.math.BigDecimal;

public record RoomFurnitureResponse(
        Long furnitureTypeId,
        String furnitureTypeCode,
        Long variantId,
        BigDecimal positionX,
        BigDecimal positionY,
        BigDecimal rotation,
        int renderOrder,
        boolean hidden
) {

    public static RoomFurnitureResponse from(RoomFurniture furniture) {
        FurnitureVariant variant = furniture.getVariant();
        FurnitureType type = variant.getFurnitureType();
        return new RoomFurnitureResponse(
                type.getId(),
                type.getCode(),
                variant.getId(),
                furniture.getPositionX(),
                furniture.getPositionY(),
                furniture.getRotation(),
                furniture.getRenderOrder(),
                !furniture.isActive()
        );
    }

    public static RoomFurnitureResponse hiddenDefault(FurnitureType type, FurnitureVariant variant) {
        return new RoomFurnitureResponse(
                type.getId(),
                type.getCode(),
                variant.getId(),
                type.getPositionX(),
                type.getPositionY(),
                type.getRotation(),
                type.getRenderOrder(),
                true
        );
    }
}

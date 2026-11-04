package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.Placement;
import com.itta.toggleme.room.domain.RoomFurniture;

// 필드 모양은 프론트 backend-room-layout.json의 가구 항목과 같다
public record RoomFurnitureResponse(
        Long furnitureTypeId,
        String furnitureTypeCode,
        Long variantId,
        String objectNodeName,
        Vector3 position,
        Quaternion rotationQuaternion,
        Vector3 scale,
        int renderOrder,
        boolean hidden
) {

    public record Vector3(double x, double y, double z) {
    }

    public record Quaternion(double x, double y, double z, double w) {
    }

    public static RoomFurnitureResponse from(RoomFurniture furniture) {
        FurnitureVariant variant = furniture.getVariant();
        return of(variant.getFurnitureType(), variant, furniture.getPlacement(), furniture.getRenderOrder(),
                !furniture.isActive());
    }

    public static RoomFurnitureResponse hiddenDefault(FurnitureType type, FurnitureVariant variant) {
        return of(type, variant, type.getPlacement(), type.getRenderOrder(), true);
    }

    private static RoomFurnitureResponse of(
            FurnitureType type,
            FurnitureVariant variant,
            Placement placement,
            int renderOrder,
            boolean hidden
    ) {
        return new RoomFurnitureResponse(
                type.getId(),
                type.getCode(),
                variant.getId(),
                variant.getObjectNodeName(),
                new Vector3(placement.positionX(), placement.positionY(), placement.positionZ()),
                new Quaternion(placement.rotationX(), placement.rotationY(), placement.rotationZ(),
                        placement.rotationW()),
                new Vector3(placement.scaleX(), placement.scaleY(), placement.scaleZ()),
                renderOrder,
                hidden
        );
    }
}

package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.FurnitureVariant;

public record FurnitureVariantResponse(
        Long id,
        Long furnitureTypeId,
        String name,
        String assetObjectKey,
        // GLB 안에서 이 디자인을 찾는 노드 이름. 디자인 선택 화면의 미리보기에 쓴다(방 가구 조회의 objectNodeName과 같다)
        String objectNodeName,
        int widthUnits,
        int heightUnits
) {

    public static FurnitureVariantResponse from(FurnitureVariant variant) {
        return new FurnitureVariantResponse(
                variant.getId(),
                variant.getFurnitureType().getId(),
                variant.getName(),
                variant.getAssetObjectKey(),
                variant.getObjectNodeName(),
                variant.getWidthUnits(),
                variant.getHeightUnits()
        );
    }
}

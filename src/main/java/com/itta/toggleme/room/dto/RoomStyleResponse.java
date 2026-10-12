package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;

public record RoomStyleResponse(
        Long id,
        RoomStyleType type,
        String name,
        String assetObjectKey
) {

    public static RoomStyleResponse from(RoomStyle style) {
        return new RoomStyleResponse(
                style.getId(),
                style.getType(),
                style.getName(),
                style.getAssetObjectKey()
        );
    }
}

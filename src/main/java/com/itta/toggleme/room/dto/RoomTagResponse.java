package com.itta.toggleme.room.dto;

// 가구 하나의 대표 태그. name은 '#' 없이 준다
public record RoomTagResponse(
        Long furnitureTypeId,
        String name
) {
}

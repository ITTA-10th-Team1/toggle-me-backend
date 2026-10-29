package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.RoomFloor;

public record RoomFloorResponse(
        Long id,
        int floorNumber,
        String name,
        Long wallStyleId,
        Long floorStyleId
) {

    public static RoomFloorResponse from(RoomFloor floor) {
        return new RoomFloorResponse(
                floor.getId(),
                floor.getFloorNumber(),
                floor.getName(),
                floor.getWallStyle().getId(),
                floor.getFloorStyle().getId()
        );
    }
}

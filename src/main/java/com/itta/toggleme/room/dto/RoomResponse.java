package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomVisibility;
import java.time.Instant;

public record RoomResponse(
        Long id,
        String name,
        RoomVisibility visibility,
        Instant createdAt
) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getVisibility(),
                room.getCreatedAt()
        );
    }
}

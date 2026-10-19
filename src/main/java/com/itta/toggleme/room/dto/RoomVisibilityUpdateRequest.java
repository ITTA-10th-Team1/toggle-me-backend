package com.itta.toggleme.room.dto;

import com.itta.toggleme.room.domain.RoomVisibility;
import jakarta.validation.constraints.NotNull;

public record RoomVisibilityUpdateRequest(
        @NotNull(message = "공개 설정은 필수입니다.")
        RoomVisibility visibility
) {
}

package com.itta.toggleme.room.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoomFurnitureUpdateRequest(
        @NotEmpty List<@NotNull @Valid Item> furniture
) {

    public record Item(
            @NotNull Long furnitureTypeId,
            @NotNull Long variantId,
            @NotNull Boolean hidden
    ) {
    }
}

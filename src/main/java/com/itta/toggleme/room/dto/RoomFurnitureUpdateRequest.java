package com.itta.toggleme.room.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoomFurnitureUpdateRequest(
        @NotEmpty(message = "가구 목록은 필수입니다.")
        List<@NotNull(message = "가구 항목은 비어 있을 수 없습니다.") @Valid Item> furniture
) {

    public record Item(
            @NotNull(message = "가구 종류 ID는 필수입니다.") Long furnitureTypeId,
            @NotNull(message = "가구 디자인 ID는 필수입니다.") Long variantId,
            @NotNull(message = "숨김 여부는 필수입니다.") Boolean hidden
    ) {
    }
}

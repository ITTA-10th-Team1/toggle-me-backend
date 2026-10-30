package com.itta.toggleme.item.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ItemVisibility {
    PUBLIC,
    PRIVATE;

    // RoomVisibility와 같이 이 enum을 쓰는 API에만 정확한 문자열 계약을 적용한다
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ItemVisibility fromJson(String value) {
        return ItemVisibility.valueOf(value);
    }
}

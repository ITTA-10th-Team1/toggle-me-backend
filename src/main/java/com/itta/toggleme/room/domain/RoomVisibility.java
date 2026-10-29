package com.itta.toggleme.room.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoomVisibility {
    SOCIAL_ON,
    SOCIAL_OFF;

    // 전역 enum 설정 대신 RoomVisibility를 사용하는 API에만 정확한 문자열 계약을 적용한다.
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RoomVisibility fromJson(String value) {
        return RoomVisibility.valueOf(value);
    }
}

package com.itta.toggleme.item.dto;

import java.time.LocalDate;

// 사진이 있는 날짜만 내려준다. thumbnailUrl은 그날 첫 사진의 보기 주소다
public record FridgeCalendarDayResponse(
        LocalDate date,
        int count,
        String thumbnailUrl
) {
}

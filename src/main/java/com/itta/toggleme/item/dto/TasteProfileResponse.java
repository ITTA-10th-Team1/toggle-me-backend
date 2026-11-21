package com.itta.toggleme.item.dto;

import java.time.LocalDate;
import java.util.List;

// 취향 카드. 기록이 requiredCount 미만이면 eligible=false이고 tags는 비어 있다
public record TasteProfileResponse(
        int periodDays,
        LocalDate from,
        LocalDate to,
        long itemCount,
        int requiredCount,
        boolean eligible,
        List<TasteTagResponse> tags
) {
}

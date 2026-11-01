package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.ItemVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ItemCreateRequest(
        @NotBlank(message = "이미지 키는 필수입니다.")
        @Size(max = 500, message = "이미지 키가 너무 깁니다.")
        String imageKey,

        @NotNull(message = "가구 종류는 필수입니다.")
        Long furnitureTypeId,

        // 앱 기기의 날짜. 비우면 오늘(Asia/Seoul)로 기록한다. 지난 날짜를 받을지는 가구별 핸들러가 정한다
        LocalDate recordedOn,

        @Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
        String memo,

        @NotNull(message = "공개 여부는 필수입니다.")
        ItemVisibility visibility,

        // AI 추천 태그 중 사용자가 고른 것. '#'는 붙여도 되고 빼도 된다. 비우거나 null이면 태그 없이 저장한다
        @Size(max = 3, message = "태그는 3개까지 고를 수 있습니다.")
        List<String> tags
) {
}

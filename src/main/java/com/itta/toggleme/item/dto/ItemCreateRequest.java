package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.ItemVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

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
        ItemVisibility visibility
) {
}

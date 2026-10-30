package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.ItemVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FridgeFoodCreateRequest(
        @NotBlank(message = "이미지 키는 필수입니다.")
        @Size(max = 500, message = "이미지 키가 너무 깁니다.")
        String imageKey,

        // 앱 기기의 날짜를 그대로 받는다. 서버(UTC)에서 계산하면 한국 시간 0~9시 사진이 전날로 들어간다
        @NotNull(message = "날짜는 필수입니다.")
        LocalDate recordedOn,

        @Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
        String memo,

        @NotNull(message = "공개 여부는 필수입니다.")
        ItemVisibility visibility
) {
}

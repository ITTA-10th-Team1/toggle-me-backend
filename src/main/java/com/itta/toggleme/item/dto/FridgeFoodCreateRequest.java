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

        // 이전 앱과의 호환용으로 받기만 하고 쓰지 않는다. 등록일은 서버가 오늘(Asia/Seoul)로 기록한다
        LocalDate recordedOn,

        @Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
        String memo,

        @NotNull(message = "공개 여부는 필수입니다.")
        ItemVisibility visibility
) {
}

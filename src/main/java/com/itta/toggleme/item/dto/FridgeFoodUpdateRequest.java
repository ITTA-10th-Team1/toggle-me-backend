package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.ItemVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 메모와 공개 여부를 함께 교체한다. 메모를 지우려면 null을 보낸다
public record FridgeFoodUpdateRequest(
        @Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
        String memo,

        @NotNull(message = "공개 여부는 필수입니다.")
        ItemVisibility visibility
) {
}

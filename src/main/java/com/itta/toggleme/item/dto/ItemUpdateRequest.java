package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.domain.ItemVisibility;
import jakarta.validation.constraints.Size;

// 보낸 값만 바꾼다. 안 보낸(null) 값은 그대로 두고, 메모를 지우려면 빈 문자열을 보낸다
public record ItemUpdateRequest(
        @Size(max = 200, message = "메모는 200자 이하로 입력해 주세요.")
        String memo,

        ItemVisibility visibility
) {
}

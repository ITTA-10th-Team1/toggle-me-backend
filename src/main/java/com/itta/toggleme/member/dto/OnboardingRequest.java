package com.itta.toggleme.member.dto;

import com.itta.toggleme.room.domain.RoomVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OnboardingRequest(
        @NotNull(message = "가구 선택은 필수입니다.")
        // MVP는 거울·냉장고 2종이라 최소 2개. 가구 5종이 되면 RoomService와 함께 4로 되돌린다
        @Size(min = 2, max = 5, message = "가구는 2개 이상 5개 이하로 선택해야 합니다.")
        List<@NotNull(message = "가구 디자인 ID는 필수입니다.") Long> furnitureVariantIds,
        @NotNull(message = "공개 설정은 필수입니다.")
        RoomVisibility visibility
) {
}

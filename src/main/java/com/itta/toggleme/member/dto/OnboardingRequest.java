package com.itta.toggleme.member.dto;

import com.itta.toggleme.room.domain.RoomVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OnboardingRequest(
        @NotNull(message = "가구 선택은 필수입니다.")
        @Size(min = 4, max = 5, message = "가구는 4개 이상 5개 이하로 선택해야 합니다.")
        List<@NotNull(message = "가구 디자인 ID는 필수입니다.") Long> furnitureVariantIds,
        @NotNull(message = "공개 설정은 필수입니다.")
        RoomVisibility visibility
) {
}

package com.itta.toggleme.item.service.handler;

import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.exception.ItemDailyLimitExceededException;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 냉장고: 하루 1장(기획 확정, 추후 조정). 카테고리 변경으로 옮겨 오는 기록에는 걸지 않는다
@Component
@RequiredArgsConstructor
public class FridgeItemHandler implements ItemRecordHandler {

    public static final String FRIDGE_CODE = "fridge";
    private static final int DAILY_LIMIT = 1;

    private final ItemRepository itemRepository;

    @Override
    public String furnitureCode() {
        return FRIDGE_CODE;
    }

    @Override
    public void validate(Long memberId, FurnitureType furnitureType, LocalDate recordedOn,
            ItemCreateRequest request) {
        if (itemRepository.countByMemberIdAndFurnitureTypeIdAndRecordedOn(
                memberId, furnitureType.getId(), recordedOn) >= DAILY_LIMIT) {
            throw new ItemDailyLimitExceededException();
        }
    }
}

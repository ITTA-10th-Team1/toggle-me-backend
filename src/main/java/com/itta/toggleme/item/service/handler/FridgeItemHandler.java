package com.itta.toggleme.item.service.handler;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.exception.FridgeNotSelectedException;
import com.itta.toggleme.item.exception.ItemDailyLimitExceededException;
import com.itta.toggleme.item.exception.ItemRecordedDateInvalidException;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 냉장고: 그날 먹은 음식 사진. 지난 날짜 가능, 미래 날짜 불가, 하루 10장
@Component
@RequiredArgsConstructor
public class FridgeItemHandler implements ItemRecordHandler {

    public static final String FRIDGE_CODE = "FRIDGE";
    private static final int DAILY_LIMIT = 10;

    private final ItemRepository itemRepository;

    @Override
    public String furnitureCode() {
        return FRIDGE_CODE;
    }

    @Override
    public LocalDate prepare(Long memberId, FurnitureType furnitureType, ItemCreateRequest request, LocalDate today) {
        LocalDate recordedOn = request.recordedOn() == null ? today : request.recordedOn();
        if (recordedOn.isAfter(today)) {
            throw new ItemRecordedDateInvalidException();
        }
        if (itemRepository.countByMemberIdAndFurnitureTypeIdAndRecordedOn(
                memberId, furnitureType.getId(), recordedOn) >= DAILY_LIMIT) {
            throw new ItemDailyLimitExceededException();
        }
        return recordedOn;
    }

    @Override
    public BusinessException furnitureNotSelected() {
        return new FridgeNotSelectedException();
    }
}

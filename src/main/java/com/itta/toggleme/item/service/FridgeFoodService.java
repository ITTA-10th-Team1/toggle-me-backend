package com.itta.toggleme.item.service;

import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.dto.FridgeCalendarDayResponse;
import com.itta.toggleme.item.dto.FridgeFoodCreateRequest;
import com.itta.toggleme.item.dto.FridgeFoodResponse;
import com.itta.toggleme.item.dto.FridgeFoodUpdateRequest;
import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.exception.ItemNotFoundException;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.item.service.handler.FridgeItemHandler;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.exception.FurnitureTypeNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FridgeFoodService {

    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final FurnitureTypeRepository furnitureTypeRepository;
    private final ImageService imageService;

    // 등록 규칙은 ItemService와 FridgeItemHandler에 있다. 이 API는 기존 앱 계약을 위해 남긴다
    @Transactional
    public FridgeFoodResponse create(Long memberId, FridgeFoodCreateRequest request) {
        Item item = itemService.record(memberId, new ItemCreateRequest(
                request.imageKey(),
                findFridge().getId(),
                request.recordedOn(),
                request.memo(),
                request.visibility(),
                List.of()
        ));
        return toResponse(item);
    }

    public List<FridgeFoodResponse> getByDate(Long memberId, LocalDate date) {
        return itemRepository.findAllByMemberIdAndFurnitureTypeIdAndRecordedOnOrderById(
                        memberId, findFridge().getId(), date).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FridgeCalendarDayResponse> getCalendar(Long memberId, YearMonth month) {
        List<Item> items = itemRepository
                .findAllByMemberIdAndFurnitureTypeIdAndRecordedOnBetweenOrderByRecordedOnAscIdAsc(
                        memberId, findFridge().getId(), month.atDay(1), month.atEndOfMonth());
        Map<LocalDate, List<Item>> itemsByDate = new LinkedHashMap<>();
        for (Item item : items) {
            itemsByDate.computeIfAbsent(item.getRecordedOn(), date -> new ArrayList<>()).add(item);
        }
        return itemsByDate.entrySet().stream()
                .map(entry -> new FridgeCalendarDayResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        imageService.createViewUrl(entry.getValue().getFirst().getImageKey())
                ))
                .toList();
    }

    @Transactional
    public FridgeFoodResponse update(Long memberId, Long itemId, FridgeFoodUpdateRequest request) {
        Item item = findMyFood(memberId, itemId);
        item.update(request.memo(), request.visibility());
        return toResponse(item);
    }

    @Transactional
    public void delete(Long memberId, Long itemId) {
        Item item = findMyFood(memberId, itemId);
        itemRepository.delete(item);
        // DB 삭제가 확정된 뒤에 S3 파일을 지운다. 롤백되면 사진은 남는다
        String imageKey = item.getImageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageService.delete(imageKey);
            }
        });
    }

    private Item findMyFood(Long memberId, Long itemId) {
        return itemRepository.findByIdAndMemberIdAndFurnitureTypeId(itemId, memberId, findFridge().getId())
                .orElseThrow(ItemNotFoundException::new);
    }

    private FurnitureType findFridge() {
        return furnitureTypeRepository.findByCode(FridgeItemHandler.FRIDGE_CODE).orElseThrow(FurnitureTypeNotFoundException::new);
    }

    private FridgeFoodResponse toResponse(Item item) {
        return FridgeFoodResponse.from(item, imageService.createViewUrl(item.getImageKey()));
    }
}

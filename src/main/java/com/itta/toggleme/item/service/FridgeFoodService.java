package com.itta.toggleme.item.service;

import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.dto.FridgeCalendarDayResponse;
import com.itta.toggleme.item.dto.FridgeFoodCreateRequest;
import com.itta.toggleme.item.dto.FridgeFoodResponse;
import com.itta.toggleme.item.dto.FridgeFoodUpdateRequest;
import com.itta.toggleme.item.exception.FridgeNotSelectedException;
import com.itta.toggleme.item.exception.ItemDailyLimitExceededException;
import com.itta.toggleme.item.exception.ItemImageInvalidException;
import com.itta.toggleme.item.exception.ItemNotFoundException;
import com.itta.toggleme.item.exception.ItemRecordedDateInvalidException;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.exception.MemberNotFoundException;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.RoomStatus;
import com.itta.toggleme.room.exception.FurnitureTypeNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.RoomFurnitureRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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

    private static final String FRIDGE_CODE = "FRIDGE";
    private static final int DAILY_LIMIT = 10;
    // 앱 사용자 기준 날짜. 해외 사용자를 받게 되면 기기 시간대를 함께 받는다
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final FurnitureTypeRepository furnitureTypeRepository;
    private final RoomFurnitureRepository roomFurnitureRepository;
    private final ImageService imageService;

    @Transactional
    public FridgeFoodResponse create(Long memberId, FridgeFoodCreateRequest request) {
        // 회원 행을 잠가 같은 회원의 동시 등록이 하루 제한을 넘지 않게 한다
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(MemberNotFoundException::new);
        FurnitureType fridge = findFridge();
        if (!roomFurnitureRepository.existsActiveByOwnerIdAndFurnitureTypeId(
                memberId, RoomStatus.ACTIVE, fridge.getId())) {
            throw new FridgeNotSelectedException();
        }
        if (request.recordedOn().isAfter(LocalDate.now(SERVICE_ZONE))) {
            throw new ItemRecordedDateInvalidException();
        }
        if (itemRepository.countByMemberIdAndFurnitureTypeIdAndRecordedOn(
                memberId, fridge.getId(), request.recordedOn()) >= DAILY_LIMIT) {
            throw new ItemDailyLimitExceededException();
        }
        if (itemRepository.existsByImageKey(request.imageKey())
                || !imageService.isUploadedBy(memberId, request.imageKey())) {
            throw new ItemImageInvalidException();
        }

        Item item = itemRepository.save(Item.record(
                member,
                fridge,
                request.recordedOn(),
                request.imageKey(),
                request.memo(),
                request.visibility()
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
        return furnitureTypeRepository.findByCode(FRIDGE_CODE).orElseThrow(FurnitureTypeNotFoundException::new);
    }

    private FridgeFoodResponse toResponse(Item item) {
        return FridgeFoodResponse.from(item, imageService.createViewUrl(item.getImageKey()));
    }
}

package com.itta.toggleme.item.service;

import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemTag;
import com.itta.toggleme.item.dto.ItemCalendarDayResponse;
import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.item.dto.ItemUpdateRequest;
import com.itta.toggleme.item.exception.ItemFurnitureNotSelectedException;
import com.itta.toggleme.item.exception.ItemImageInvalidException;
import com.itta.toggleme.item.exception.ItemNotFoundException;
import com.itta.toggleme.item.exception.ItemTagInvalidException;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.item.repository.ItemTagRepository;
import com.itta.toggleme.item.service.handler.ItemRecordHandler;
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
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 모든 가구의 아이템 등록·조회·수정·삭제가 거치는 곳. 가구별 규칙은 ItemRecordHandler로 분기한다
@Slf4j
@Service
@Transactional(readOnly = true)
public class ItemService {

    // 앱 사용자 기준 날짜. 해외 사용자를 받게 되면 기기 시간대를 함께 받는다
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MAX_TAG_LENGTH = 20;
    private static final int ROOM_ITEMS_PAGE_SIZE = 20;

    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;
    private final MemberRepository memberRepository;
    private final FurnitureTypeRepository furnitureTypeRepository;
    private final RoomFurnitureRepository roomFurnitureRepository;
    private final ImageService imageService;
    private final Map<String, ItemRecordHandler> handlers;

    public ItemService(
            ItemRepository itemRepository,
            ItemTagRepository itemTagRepository,
            MemberRepository memberRepository,
            FurnitureTypeRepository furnitureTypeRepository,
            RoomFurnitureRepository roomFurnitureRepository,
            ImageService imageService,
            List<ItemRecordHandler> handlers
    ) {
        this.itemRepository = itemRepository;
        this.itemTagRepository = itemTagRepository;
        this.memberRepository = memberRepository;
        this.furnitureTypeRepository = furnitureTypeRepository;
        this.roomFurnitureRepository = roomFurnitureRepository;
        this.imageService = imageService;
        // 같은 가구 code의 핸들러가 둘이면 기동할 때 실패한다
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(ItemRecordHandler::furnitureCode, Function.identity()));
    }

    // 방에서 가구를 눌렀을 때 보는 기록. 꺼진 가구는 404. 주인이 아니면 공개 기록만
    public List<ItemResponse> getRoomItems(Long ownerId, boolean includePrivate, Long furnitureTypeId, Long before) {
        if (!roomFurnitureRepository.existsActiveByOwnerIdAndFurnitureTypeId(ownerId, RoomStatus.ACTIVE, furnitureTypeId)) {
            throw new FurnitureTypeNotFoundException();
        }
        List<Item> items = itemRepository.findRoomItems(
                ownerId, furnitureTypeId, includePrivate, before, Limit.of(ROOM_ITEMS_PAGE_SIZE));
        Map<Long, List<String>> tagsByItem = itemTagRepository
                .findAllByItemIdInOrderByPosition(items.stream().map(Item::getId).toList()).stream()
                .collect(Collectors.groupingBy(tag -> tag.getItem().getId(),
                        Collectors.mapping(ItemTag::getName, Collectors.toList())));
        return items.stream()
                .map(item -> ItemResponse.from(item, imageService.createViewUrl(item.getImageKey()),
                        tagsByItem.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ItemResponse create(Long memberId, ItemCreateRequest request) {
        Item item = record(memberId, request);
        List<String> tags = itemTagRepository.findAllByItemIdOrderByPosition(item.getId()).stream()
                .map(ItemTag::getName)
                .toList();
        return ItemResponse.from(item, imageService.createViewUrl(item.getImageKey()), tags);
    }

    @Transactional
    public Item record(Long memberId, ItemCreateRequest request) {
        List<String> tagNames = normalizeTags(request.tags());
        // 회원 행을 잠가 같은 회원의 동시 등록이 가구별 개수 제한을 넘지 않게 한다
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(MemberNotFoundException::new);
        FurnitureType furnitureType = furnitureTypeRepository.findById(request.furnitureTypeId())
                .orElseThrow(FurnitureTypeNotFoundException::new);
        ItemRecordHandler handler = handlers.get(furnitureType.getCode());

        if (!roomFurnitureRepository.existsActiveByOwnerIdAndFurnitureTypeId(
                memberId, RoomStatus.ACTIVE, furnitureType.getId())) {
            throw handler == null ? new ItemFurnitureNotSelectedException() : handler.furnitureNotSelected();
        }
        if (handler != null) {
            handler.validate(memberId, furnitureType, request);
        }
        if (itemRepository.existsByImageKey(request.imageKey())
                || !imageService.isUploadedBy(memberId, request.imageKey())) {
            throw new ItemImageInvalidException();
        }

        Item item = itemRepository.save(Item.record(
                member,
                furnitureType,
                LocalDate.now(SERVICE_ZONE),
                request.imageKey(),
                request.memo(),
                request.visibility()
        ));
        for (int position = 0; position < tagNames.size(); position++) {
            itemTagRepository.save(ItemTag.of(item, tagNames.get(position), position));
        }
        return item;
    }

    // 그날 기록한 아이템. 가구가 방에 켜져 있는지는 보지 않는다(방에서 뺀 뒤에도 기록은 볼 수 있다)
    public List<ItemResponse> getByDate(Long memberId, Long furnitureTypeId, LocalDate date) {
        requireFurnitureType(furnitureTypeId);
        List<Item> items = itemRepository.findAllByMemberIdAndFurnitureTypeIdAndRecordedOnOrderById(
                memberId, furnitureTypeId, date);
        return toResponses(items);
    }

    public List<ItemCalendarDayResponse> getCalendar(Long memberId, Long furnitureTypeId, YearMonth month) {
        requireFurnitureType(furnitureTypeId);
        List<Item> items = itemRepository
                .findAllByMemberIdAndFurnitureTypeIdAndRecordedOnBetweenOrderByRecordedOnAscIdAsc(
                        memberId, furnitureTypeId, month.atDay(1), month.atEndOfMonth());
        Map<LocalDate, List<Item>> itemsByDate = new LinkedHashMap<>();
        for (Item item : items) {
            itemsByDate.computeIfAbsent(item.getRecordedOn(), date -> new ArrayList<>()).add(item);
        }
        return itemsByDate.entrySet().stream()
                .map(entry -> new ItemCalendarDayResponse(
                        entry.getKey(),
                        entry.getValue().size(),
                        imageService.createViewUrl(entry.getValue().getFirst().getImageKey())
                ))
                .toList();
    }

    // 메모와 공개 여부만 바꾼다. 가구·날짜·사진·태그는 바꾸지 않는다
    @Transactional
    public ItemResponse update(Long memberId, Long itemId, ItemUpdateRequest request) {
        Item item = findMyItem(memberId, itemId);
        item.update(request.memo(), request.visibility());
        return toResponses(List.of(item)).getFirst();
    }

    @Transactional
    public void delete(Long memberId, Long itemId) {
        Item item = findMyItem(memberId, itemId);
        itemRepository.delete(item);
        // DB 삭제가 확정된 뒤에 S3 파일을 지운다. 롤백되면 사진은 남는다
        String imageKey = item.getImageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // DB는 이미 지워졌으므로 S3 삭제가 실패해도 오류로 응답하지 않는다(앱이 재시도하면 404가 된다).
                // 사진 키는 로그에 남기지 않는다
                try {
                    imageService.delete(imageKey);
                } catch (RuntimeException e) {
                    log.warn("Item image delete failed: {}", e.getClass().getSimpleName());
                }
            }
        });
    }

    private Item findMyItem(Long memberId, Long itemId) {
        return itemRepository.findByIdAndMemberId(itemId, memberId).orElseThrow(ItemNotFoundException::new);
    }

    private void requireFurnitureType(Long furnitureTypeId) {
        if (!furnitureTypeRepository.existsById(furnitureTypeId)) {
            throw new FurnitureTypeNotFoundException();
        }
    }

    // 태그는 한 번에 모아 조회한다
    private List<ItemResponse> toResponses(List<Item> items) {
        Map<Long, List<String>> tagsByItemId = itemTagRepository
                .findAllByItemIdInOrderByPosition(items.stream().map(Item::getId).toList()).stream()
                .collect(Collectors.groupingBy(tag -> tag.getItem().getId(),
                        Collectors.mapping(ItemTag::getName, Collectors.toList())));
        return items.stream()
                .map(item -> ItemResponse.from(item, imageService.createViewUrl(item.getImageKey()),
                        tagsByItemId.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    // '#'와 앞뒤 공백을 떼고 소문자로 바꾼 뒤 같은 태그는 하나만 남긴다. 같은 태그가 같은 값으로 저장돼야 방 태그 집계가 맞는다
    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(tag -> {
                    String name = tag == null ? "" : tag.strip().replaceFirst("^#+", "").strip().toLowerCase(Locale.ROOT);
                    if (name.isEmpty() || name.length() > MAX_TAG_LENGTH) {
                        throw new ItemTagInvalidException();
                    }
                    return name;
                })
                .distinct()
                .toList();
    }
}

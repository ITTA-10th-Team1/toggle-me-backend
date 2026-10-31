package com.itta.toggleme.item.service;

import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.item.exception.ItemFurnitureNotSelectedException;
import com.itta.toggleme.item.exception.ItemImageInvalidException;
import com.itta.toggleme.item.repository.ItemRepository;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 모든 가구의 아이템 등록이 거치는 곳. 가구별 규칙은 ItemRecordHandler로 분기한다
@Service
@Transactional(readOnly = true)
public class ItemService {

    // 앱 사용자 기준 날짜. 해외 사용자를 받게 되면 기기 시간대를 함께 받는다
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final FurnitureTypeRepository furnitureTypeRepository;
    private final RoomFurnitureRepository roomFurnitureRepository;
    private final ImageService imageService;
    private final Map<String, ItemRecordHandler> handlers;

    public ItemService(
            ItemRepository itemRepository,
            MemberRepository memberRepository,
            FurnitureTypeRepository furnitureTypeRepository,
            RoomFurnitureRepository roomFurnitureRepository,
            ImageService imageService,
            List<ItemRecordHandler> handlers
    ) {
        this.itemRepository = itemRepository;
        this.memberRepository = memberRepository;
        this.furnitureTypeRepository = furnitureTypeRepository;
        this.roomFurnitureRepository = roomFurnitureRepository;
        this.imageService = imageService;
        // 같은 가구 code의 핸들러가 둘이면 기동할 때 실패한다
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(ItemRecordHandler::furnitureCode, Function.identity()));
    }

    @Transactional
    public ItemResponse create(Long memberId, ItemCreateRequest request) {
        Item item = record(memberId, request);
        return ItemResponse.from(item, imageService.createViewUrl(item.getImageKey()));
    }

    @Transactional
    public Item record(Long memberId, ItemCreateRequest request) {
        // 회원 행을 잠가 같은 회원의 동시 등록이 가구별 개수 제한을 넘지 않게 한다
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(MemberNotFoundException::new);
        FurnitureType furnitureType = furnitureTypeRepository.findById(request.furnitureTypeId())
                .orElseThrow(FurnitureTypeNotFoundException::new);
        ItemRecordHandler handler = handlers.get(furnitureType.getCode());

        if (!roomFurnitureRepository.existsActiveByOwnerIdAndFurnitureTypeId(
                memberId, RoomStatus.ACTIVE, furnitureType.getId())) {
            throw handler == null ? new ItemFurnitureNotSelectedException() : handler.furnitureNotSelected();
        }
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        LocalDate recordedOn = handler == null
                ? today
                : handler.prepare(memberId, furnitureType, request, today);
        if (itemRepository.existsByImageKey(request.imageKey())
                || !imageService.isUploadedBy(memberId, request.imageKey())) {
            throw new ItemImageInvalidException();
        }

        return itemRepository.save(Item.record(
                member,
                furnitureType,
                recordedOn,
                request.imageKey(),
                request.memo(),
                request.visibility()
        ));
    }
}

package com.itta.toggleme.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.RoomFloor;
import com.itta.toggleme.room.domain.RoomFurniture;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.dto.RoomFurnitureResponse;
import com.itta.toggleme.room.dto.RoomFurnitureUpdateRequest;
import com.itta.toggleme.room.exception.FurniturePlacementNotConfiguredException;
import com.itta.toggleme.room.exception.FurnitureVariantNotFoundException;
import com.itta.toggleme.room.exception.InvalidFurnitureSelectionException;
import com.itta.toggleme.room.exception.RoomStyleNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomFloorRepository;
import com.itta.toggleme.room.repository.RoomFurnitureRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomFloorRepository roomFloorRepository;

    @Autowired
    private RoomStyleRepository roomStyleRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private FurnitureVariantRepository furnitureVariantRepository;

    @Autowired
    private RoomFurnitureRepository roomFurnitureRepository;

    @Test
    void 공개_설정을_지정해_방과_1층을_생성한다() {
        saveDefaultStyles();
        Member owner = saveMember();

        Long roomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF);

        assertThat(roomRepository.findById(roomId)).hasValueSatisfying(room -> {
            assertThat(room.getName()).isEqualTo("나의 방");
            assertThat(room.getVisibility()).isEqualTo(RoomVisibility.SOCIAL_OFF);
        });
        List<RoomFloor> floors = roomFloorRepository.findAllByRoomIdOrderByFloorNumber(roomId);
        assertThat(floors).singleElement().satisfies(floor -> {
            assertThat(floor.getFloorNumber()).isEqualTo(1);
            assertThat(floor.getName()).isEqualTo("1층");
        });
    }

    @Test
    void 공개_방을_생성한다() {
        saveDefaultStyles();
        Member owner = saveMember();

        Long roomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_ON);

        assertThat(roomRepository.findById(roomId)).hasValueSatisfying(room ->
                assertThat(room.getVisibility()).isEqualTo(RoomVisibility.SOCIAL_ON));
    }

    @Test
    void 이미_방이_있으면_다른_공개_설정으로_재요청해도_기존_방을_유지한다() {
        saveDefaultStyles();
        Member owner = saveMember();
        Long firstRoomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF);

        Long secondRoomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_ON);

        assertThat(secondRoomId).isEqualTo(firstRoomId);
        assertThat(roomRepository.findAllByOwnerIdOrderById(owner.getId())).singleElement().satisfies(room ->
                assertThat(room.getVisibility()).isEqualTo(RoomVisibility.SOCIAL_OFF));
    }

    @Test
    void 기본_스타일이_없으면_방을_만들_수_없다() {
        Member owner = saveMember();

        assertThatThrownBy(() ->
                roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF))
                .isInstanceOf(RoomStyleNotFoundException.class);
    }

    @Test
    void 네_개_행_방은_누락_종류를_숨김_기본값으로_합성한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants().subList(0, 4));

        List<RoomFurnitureResponse> response = roomService.getFurniture(
                room.owner().getId(), room.roomId(), room.floorId());

        assertThat(response).hasSize(5);
        assertThat(response.get(4).variantId()).isEqualTo(catalog.variants().get(4).getId());
        assertThat(response.get(4).hidden()).isTrue();
    }

    @Test
    void 행이_없는_방은_모든_종류를_숨김으로_합성한다() {
        saveCatalog(true);
        RoomSetup room = saveRoom(List.of());

        List<RoomFurnitureResponse> response = roomService.getFurniture(
                room.owner().getId(), room.roomId(), room.floorId());

        assertThat(response).hasSize(5).allMatch(RoomFurnitureResponse::hidden);
    }

    @Test
    void 디자인과_숨김을_변경하고_다시_표시한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(0, item(catalog.types().get(0), catalog.alternates().get(0), false));
        items.set(4, item(catalog.types().get(4), catalog.variants().get(4), true));

        List<RoomFurnitureResponse> hidden = roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(items));
        items.set(4, item(catalog.types().get(4), catalog.variants().get(4), false));
        List<RoomFurnitureResponse> shown = roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(items));

        assertThat(hidden.get(0).variantId()).isEqualTo(catalog.alternates().get(0).getId());
        assertThat(hidden.get(4).hidden()).isTrue();
        assertThat(shown.get(4).hidden()).isFalse();
    }

    @Test
    void 없는_종류_행은_첫_저장에_생성하고_이후_조회가_같다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants().subList(0, 4));

        List<RoomFurnitureResponse> updated = roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(validItems(catalog)));
        List<RoomFurnitureResponse> found = roomService.getFurniture(
                room.owner().getId(), room.roomId(), room.floorId());

        assertThat(roomFurnitureRepository.findAllByFloorId(room.floorId())).hasSize(5);
        assertThat(found).isEqualTo(updated);
    }

    @Test
    void 카테고리_누락은_거부한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());

        assertInvalidSelection(room, validItems(catalog).subList(0, 4));
    }

    @Test
    void 카테고리_중복은_거부한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(4, item(catalog.types().get(0), catalog.variants().get(4), false));

        assertInvalidSelection(room, items);
    }

    @Test
    void 디자인_ID_중복은_거부한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(4, item(catalog.types().get(4), catalog.variants().get(0), false));

        assertInvalidSelection(room, items);
    }

    @Test
    void 표시가_세_개면_거부한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(3, item(catalog.types().get(3), catalog.variants().get(3), true));
        items.set(4, item(catalog.types().get(4), catalog.variants().get(4), true));

        assertInvalidSelection(room, items);
    }

    @Test
    void 숨김이_두_개면_거부한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(0, item(catalog.types().get(0), catalog.variants().get(0), true));
        items.set(1, item(catalog.types().get(1), catalog.variants().get(1), true));

        assertInvalidSelection(room, items);
    }

    @Test
    void 다른_카테고리_디자인은_거부하고_기존_행을_유지한다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(0, item(catalog.types().get(0), catalog.variants().get(1), false));

        assertInvalidSelection(room, items);

        RoomFurniture existing = roomFurnitureRepository.findAllByFloorId(room.floorId()).stream()
                .filter(it -> it.getVariant().getFurnitureType().getId().equals(catalog.types().get(0).getId()))
                .findFirst().orElseThrow();
        assertThat(existing.getVariant().getId()).isEqualTo(catalog.variants().get(0).getId());
    }

    @Test
    void 없는_디자인은_찾을_수_없다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(0, new RoomFurnitureUpdateRequest.Item(catalog.types().get(0).getId(), Long.MAX_VALUE, false));

        assertThatThrownBy(() -> roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(items)))
                .isInstanceOf(FurnitureVariantNotFoundException.class);
    }

    @Test
    void 비활성_디자인은_찾을_수_없다() {
        Catalog catalog = saveCatalog(true);
        RoomSetup room = saveRoom(catalog.variants());
        FurnitureVariant inactive = FurnitureVariant.create(
                catalog.types().get(0), "비활성", uniqueCode(), 1, 1);
        ReflectionTestUtils.setField(inactive, "status", CatalogStatus.INACTIVE);
        inactive = furnitureVariantRepository.save(inactive);
        List<RoomFurnitureUpdateRequest.Item> items = validItems(catalog);
        items.set(0, item(catalog.types().get(0), inactive, false));

        assertThatThrownBy(() -> roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(items)))
                .isInstanceOf(FurnitureVariantNotFoundException.class);
    }

    @Test
    void 배치값_없는_종류의_새_행은_생성하지_않는다() {
        Catalog catalog = saveCatalog(false);
        RoomSetup room = saveRoom(catalog.variants().subList(0, 4));

        assertThatThrownBy(() -> roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(validItems(catalog))))
                .isInstanceOf(FurniturePlacementNotConfiguredException.class);
        assertThat(roomFurnitureRepository.findAllByFloorId(room.floorId())).hasSize(4);
    }

    private void saveDefaultStyles() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
    }

    private Member saveMember() {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(Member.create(handle, "토글러"));
    }

    private Catalog saveCatalog(boolean configureLast) {
        List<FurnitureType> types = new ArrayList<>();
        List<FurnitureVariant> variants = new ArrayList<>();
        List<FurnitureVariant> alternates = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            FurnitureType type = FurnitureType.create(uniqueCode(), "가구 " + i, i);
            if (i < 5 || configureLast) {
                type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
            }
            types.add(furnitureTypeRepository.save(type));
            variants.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "기본 " + i, uniqueCode(), 1, 1)));
            alternates.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "대안 " + i, uniqueCode(), 1, 1)));
        }
        return new Catalog(types, variants, alternates);
    }

    private RoomSetup saveRoom(List<FurnitureVariant> variants) {
        saveDefaultStyles();
        Member owner = saveMember();
        Long roomId = roomService.createDefaultRoom(
                owner,
                variants.stream().map(FurnitureVariant::getId).toList(),
                RoomVisibility.SOCIAL_OFF
        );
        Long floorId = roomFloorRepository.findAllByRoomIdOrderByFloorNumber(roomId).getFirst().getId();
        return new RoomSetup(owner, roomId, floorId);
    }

    private List<RoomFurnitureUpdateRequest.Item> validItems(Catalog catalog) {
        List<RoomFurnitureUpdateRequest.Item> items = new ArrayList<>();
        for (int i = 0; i < catalog.types().size(); i++) {
            items.add(item(catalog.types().get(i), catalog.variants().get(i), false));
        }
        return items;
    }

    private RoomFurnitureUpdateRequest.Item item(
            FurnitureType type,
            FurnitureVariant variant,
            boolean hidden
    ) {
        return new RoomFurnitureUpdateRequest.Item(type.getId(), variant.getId(), hidden);
    }

    private RoomFurnitureUpdateRequest request(List<RoomFurnitureUpdateRequest.Item> items) {
        return new RoomFurnitureUpdateRequest(items);
    }

    private void assertInvalidSelection(RoomSetup room, List<RoomFurnitureUpdateRequest.Item> items) {
        assertThatThrownBy(() -> roomService.updateFurniture(
                room.owner().getId(), room.roomId(), room.floorId(), request(items)))
                .isInstanceOf(InvalidFurnitureSelectionException.class);
    }

    private String uniqueCode() {
        return UUID.randomUUID().toString();
    }

    private record Catalog(
            List<FurnitureType> types,
            List<FurnitureVariant> variants,
            List<FurnitureVariant> alternates
    ) {
    }

    private record RoomSetup(Member owner, Long roomId, Long floorId) {
    }
}

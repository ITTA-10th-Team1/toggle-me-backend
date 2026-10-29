package com.itta.toggleme.room.service;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomFloor;
import com.itta.toggleme.room.domain.RoomFurniture;
import com.itta.toggleme.room.domain.RoomStatus;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.dto.RoomFloorResponse;
import com.itta.toggleme.room.dto.RoomFurnitureResponse;
import com.itta.toggleme.room.dto.RoomFurnitureUpdateRequest;
import com.itta.toggleme.room.dto.RoomResponse;
import com.itta.toggleme.room.exception.FurniturePlacementNotConfiguredException;
import com.itta.toggleme.room.exception.FurnitureVariantNotFoundException;
import com.itta.toggleme.room.exception.InvalidFurnitureSelectionException;
import com.itta.toggleme.room.exception.RoomNotFoundException;
import com.itta.toggleme.room.exception.RoomStyleNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomFloorRepository;
import com.itta.toggleme.room.repository.RoomFurnitureRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final String DEFAULT_ROOM_NAME = "나의 방";
    private static final String DEFAULT_FLOOR_NAME = "1층";
    private static final int FIRST_FLOOR_NUMBER = 1;
    private static final int MIN_FURNITURE_COUNT = 4;
    private static final int MAX_FURNITURE_COUNT = 5;

    private final RoomRepository roomRepository;
    private final RoomFloorRepository roomFloorRepository;
    private final RoomFurnitureRepository roomFurnitureRepository;
    private final RoomStyleRepository roomStyleRepository;
    private final FurnitureTypeRepository furnitureTypeRepository;
    private final FurnitureVariantRepository furnitureVariantRepository;

    @Transactional
    public Long createDefaultRoom(
            Member owner,
            List<Long> furnitureVariantIds,
            RoomVisibility visibility
    ) {
        List<FurnitureVariant> variants = findSelectedVariants(furnitureVariantIds);
        return roomRepository.findFirstByOwnerIdOrderById(owner.getId())
                .map(Room::getId)
                .orElseGet(() -> createRoomWithFirstFloor(owner, variants, visibility));
    }

    public List<RoomResponse> getMyRooms(Long memberId) {
        return roomRepository.findAllByOwnerIdAndStatusOrderById(memberId, RoomStatus.ACTIVE).stream()
                .map(RoomResponse::from)
                .toList();
    }

    public RoomResponse getMyRoom(Long memberId, Long roomId) {
        return RoomResponse.from(findMyRoom(memberId, roomId));
    }

    public List<RoomFloorResponse> getFloors(Long memberId, Long roomId) {
        Room room = findMyRoom(memberId, roomId);
        return roomFloorRepository.findAllByRoomIdOrderByFloorNumber(room.getId()).stream()
                .map(RoomFloorResponse::from)
                .toList();
    }

    public List<RoomFurnitureResponse> getFurniture(Long memberId, Long roomId, Long floorId) {
        Room room = findMyRoom(memberId, roomId);
        RoomFloor floor = findFloor(room.getId(), floorId);
        List<FurnitureType> types = findActiveFurnitureTypes();
        return furnitureResponses(types, roomFurnitureRepository.findAllByFloorId(floor.getId()));
    }

    @Transactional
    public List<RoomFurnitureResponse> updateFurniture(
            Long memberId,
            Long roomId,
            Long floorId,
            RoomFurnitureUpdateRequest request
    ) {
        Room room = roomRepository.findForUpdateByIdAndOwnerIdAndStatus(roomId, memberId, RoomStatus.ACTIVE)
                .orElseThrow(RoomNotFoundException::new);
        RoomFloor floor = findFloor(room.getId(), floorId);
        List<FurnitureType> types = findActiveFurnitureTypes();
        Map<Long, RoomFurnitureUpdateRequest.Item> itemsByType = validateSelection(types, request.furniture());
        Map<Long, FurnitureVariant> variantsById = findRequestedVariants(request.furniture());
        validateVariantTypes(request.furniture(), variantsById);

        Map<Long, RoomFurniture> furnitureByType = furnitureByType(
                roomFurnitureRepository.findAllByFloorId(floor.getId())
        );
        List<RoomFurniture> furniture = new ArrayList<>();
        for (FurnitureType type : types) {
            RoomFurniture roomFurniture = furnitureByType.get(type.getId());
            if (roomFurniture == null) {
                if (!type.hasConfiguredPlacement()) {
                    throw new FurniturePlacementNotConfiguredException();
                }
                roomFurniture = placeFurniture(floor, variantsById.get(itemsByType.get(type.getId()).variantId()));
            }
            RoomFurnitureUpdateRequest.Item item = itemsByType.get(type.getId());
            roomFurniture.customize(variantsById.get(item.variantId()), item.hidden());
            furniture.add(roomFurniture);
        }
        roomFurnitureRepository.saveAllAndFlush(furniture);
        return furnitureResponses(types, furniture);
    }

    @Transactional
    public RoomResponse changeVisibility(Long memberId, Long roomId, RoomVisibility visibility) {
        Room room = findMyRoom(memberId, roomId);
        room.changeVisibility(visibility);
        return RoomResponse.from(room);
    }

    private Long createRoomWithFirstFloor(
            Member owner,
            List<FurnitureVariant> variants,
            RoomVisibility visibility
    ) {
        RoomStyle wallStyle = findDefaultStyle(RoomStyleType.WALL);
        RoomStyle floorStyle = findDefaultStyle(RoomStyleType.FLOOR);

        Room room = roomRepository.save(Room.create(owner, DEFAULT_ROOM_NAME, visibility));
        RoomFloor floor = roomFloorRepository.save(
                RoomFloor.create(room, wallStyle, floorStyle, FIRST_FLOOR_NUMBER, DEFAULT_FLOOR_NAME)
        );
        roomFurnitureRepository.saveAll(variants.stream()
                .map(variant -> placeFurniture(floor, variant))
                .toList());
        return room.getId();
    }

    private List<FurnitureVariant> findSelectedVariants(List<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of();
        }
        if (variantIds.size() < MIN_FURNITURE_COUNT || variantIds.size() > MAX_FURNITURE_COUNT
                || new HashSet<>(variantIds).size() != variantIds.size()) {
            throw new InvalidFurnitureSelectionException();
        }

        List<FurnitureVariant> variants = furnitureVariantRepository.findAllByIdInAndStatus(
                variantIds,
                CatalogStatus.ACTIVE
        );
        if (variants.size() != variantIds.size()) {
            throw new FurnitureVariantNotFoundException();
        }

        Set<Long> typeIds = new HashSet<>();
        for (FurnitureVariant variant : variants) {
            FurnitureType type = variant.getFurnitureType();
            if (!typeIds.add(type.getId())) {
                throw new InvalidFurnitureSelectionException();
            }
            if (!type.hasConfiguredPlacement()) {
                throw new FurniturePlacementNotConfiguredException();
            }
        }
        return variants;
    }

    private RoomFurniture placeFurniture(RoomFloor floor, FurnitureVariant variant) {
        FurnitureType type = variant.getFurnitureType();
        return RoomFurniture.place(
                floor,
                variant,
                type.getPositionX(),
                type.getPositionY(),
                type.getRotation(),
                type.getRenderOrder()
        );
    }

    private RoomStyle findDefaultStyle(RoomStyleType type) {
        return roomStyleRepository.findAllByTypeAndStatusOrderById(type, CatalogStatus.ACTIVE).stream()
                .findFirst()
                .orElseThrow(RoomStyleNotFoundException::new);
    }

    private List<FurnitureType> findActiveFurnitureTypes() {
        return furnitureTypeRepository.findAllByStatusOrderByDisplayOrder(CatalogStatus.ACTIVE);
    }

    private RoomFloor findFloor(Long roomId, Long floorId) {
        return roomFloorRepository.findByIdAndRoomId(floorId, roomId)
                .orElseThrow(RoomNotFoundException::new);
    }

    private List<RoomFurnitureResponse> furnitureResponses(
            List<FurnitureType> types,
            List<RoomFurniture> furniture
    ) {
        Map<Long, RoomFurniture> furnitureByType = furnitureByType(furniture);
        return types.stream()
                .map(type -> {
                    RoomFurniture existing = furnitureByType.get(type.getId());
                    if (existing != null) {
                        return RoomFurnitureResponse.from(existing);
                    }
                    if (!type.hasConfiguredPlacement()) {
                        throw new FurniturePlacementNotConfiguredException();
                    }
                    FurnitureVariant variant = furnitureVariantRepository
                            .findFirstByFurnitureTypeIdAndStatusOrderById(type.getId(), CatalogStatus.ACTIVE)
                            .orElseThrow(FurnitureVariantNotFoundException::new);
                    return RoomFurnitureResponse.hiddenDefault(type, variant);
                })
                .sorted(Comparator.comparingInt(RoomFurnitureResponse::renderOrder)
                        .thenComparing(RoomFurnitureResponse::furnitureTypeId))
                .toList();
    }

    private Map<Long, RoomFurnitureUpdateRequest.Item> validateSelection(
            List<FurnitureType> types,
            List<RoomFurnitureUpdateRequest.Item> items
    ) {
        Set<Long> activeTypeIds = types.stream().map(FurnitureType::getId).collect(Collectors.toSet());
        Set<Long> requestedTypeIds = items.stream()
                .map(RoomFurnitureUpdateRequest.Item::furnitureTypeId)
                .collect(Collectors.toSet());
        Set<Long> variantIds = items.stream()
                .map(RoomFurnitureUpdateRequest.Item::variantId)
                .collect(Collectors.toSet());
        long hiddenCount = items.stream().filter(item -> item.hidden()).count();
        if (items.size() != requestedTypeIds.size()
                || !requestedTypeIds.equals(activeTypeIds)
                || items.size() != variantIds.size()
                || items.size() - hiddenCount < MIN_FURNITURE_COUNT
                || hiddenCount > 1) {
            throw new InvalidFurnitureSelectionException();
        }
        return items.stream().collect(Collectors.toMap(
                RoomFurnitureUpdateRequest.Item::furnitureTypeId,
                Function.identity()
        ));
    }

    private Map<Long, FurnitureVariant> findRequestedVariants(List<RoomFurnitureUpdateRequest.Item> items) {
        List<Long> variantIds = items.stream().map(RoomFurnitureUpdateRequest.Item::variantId).toList();
        List<FurnitureVariant> variants = furnitureVariantRepository.findAllByIdInAndStatus(
                variantIds,
                CatalogStatus.ACTIVE
        );
        if (variants.size() != variantIds.size()) {
            throw new FurnitureVariantNotFoundException();
        }
        return variants.stream().collect(Collectors.toMap(FurnitureVariant::getId, Function.identity()));
    }

    private void validateVariantTypes(
            List<RoomFurnitureUpdateRequest.Item> items,
            Map<Long, FurnitureVariant> variantsById
    ) {
        if (items.stream().anyMatch(item -> !variantsById.get(item.variantId()).getFurnitureType().getId()
                .equals(item.furnitureTypeId()))) {
            throw new InvalidFurnitureSelectionException();
        }
    }

    private Map<Long, RoomFurniture> furnitureByType(List<RoomFurniture> furniture) {
        Map<Long, RoomFurniture> result = new HashMap<>();
        for (RoomFurniture roomFurniture : furniture) {
            Long typeId = roomFurniture.getVariant().getFurnitureType().getId();
            if (result.put(typeId, roomFurniture) != null) {
                throw new InvalidFurnitureSelectionException();
            }
        }
        return result;
    }

    private Room findMyRoom(Long memberId, Long roomId) {
        return roomRepository.findByIdAndOwnerIdAndStatus(roomId, memberId, RoomStatus.ACTIVE)
                .orElseThrow(RoomNotFoundException::new);
    }
}

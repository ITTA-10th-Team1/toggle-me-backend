package com.itta.toggleme.room.service;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomFloor;
import com.itta.toggleme.room.domain.RoomStatus;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.dto.RoomFloorResponse;
import com.itta.toggleme.room.dto.RoomResponse;
import com.itta.toggleme.room.exception.RoomNotFoundException;
import com.itta.toggleme.room.exception.RoomStyleNotFoundException;
import com.itta.toggleme.room.repository.RoomFloorRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.util.List;
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

    private final RoomRepository roomRepository;
    private final RoomFloorRepository roomFloorRepository;
    private final RoomStyleRepository roomStyleRepository;

    @Transactional
    public Long createDefaultRoom(Member owner) {
        return roomRepository.findFirstByOwnerIdOrderById(owner.getId())
                .map(Room::getId)
                .orElseGet(() -> createRoomWithFirstFloor(owner));
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

    @Transactional
    public RoomResponse changeVisibility(Long memberId, Long roomId, RoomVisibility visibility) {
        Room room = findMyRoom(memberId, roomId);
        room.changeVisibility(visibility);
        return RoomResponse.from(room);
    }

    private Long createRoomWithFirstFloor(Member owner) {
        RoomStyle wallStyle = findDefaultStyle(RoomStyleType.WALL);
        RoomStyle floorStyle = findDefaultStyle(RoomStyleType.FLOOR);

        Room room = roomRepository.save(Room.create(owner, DEFAULT_ROOM_NAME));
        roomFloorRepository.save(RoomFloor.create(room, wallStyle, floorStyle, FIRST_FLOOR_NUMBER, DEFAULT_FLOOR_NAME));
        return room.getId();
    }

    private RoomStyle findDefaultStyle(RoomStyleType type) {
        return roomStyleRepository.findAllByTypeAndStatusOrderById(type, CatalogStatus.ACTIVE).stream()
                .findFirst()
                .orElseThrow(RoomStyleNotFoundException::new);
    }

    private Room findMyRoom(Long memberId, Long roomId) {
        return roomRepository.findByIdAndOwnerIdAndStatus(roomId, memberId, RoomStatus.ACTIVE)
                .orElseThrow(RoomNotFoundException::new);
    }
}

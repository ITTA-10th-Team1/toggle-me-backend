package com.itta.toggleme.follow.service;

import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.item.service.ItemService;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.room.dto.SharedRoomResponse;
import com.itta.toggleme.room.service.RoomService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 다른 회원의 방 보기(ROOM-35~37). 조회만 한다. 방 주인 본인이 열면 비공개 기록도 함께 준다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharedRoomService {

    private final VisibleMemberService visibleMemberService;
    private final RoomService roomService;
    private final ItemService itemService;

    public SharedRoomResponse getRoom(Long viewerId, String handle) {
        return roomService.getSharedRoom(visibleMemberService.find(viewerId, handle).getId());
    }

    public List<ItemResponse> getItems(Long viewerId, String handle, Long furnitureTypeId, Long before) {
        Member owner = visibleMemberService.find(viewerId, handle);
        return itemService.getRoomItems(owner.getId(), owner.getId().equals(viewerId), furnitureTypeId, before);
    }
}

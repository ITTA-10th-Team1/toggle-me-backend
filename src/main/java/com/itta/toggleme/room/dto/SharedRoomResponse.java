package com.itta.toggleme.room.dto;

import java.util.List;

// 다른 회원이 보는 방 한 화면: 1층 벽·바닥, 켜진 가구 배치, 방 태그
public record SharedRoomResponse(
        Long roomId,
        String name,
        Long wallStyleId,
        Long floorStyleId,
        List<RoomFurnitureResponse> furniture,
        List<RoomTagResponse> tags
) {
}

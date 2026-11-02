package com.itta.toggleme.room.controller;

import com.itta.toggleme.room.dto.RoomFloorResponse;
import com.itta.toggleme.room.dto.RoomFurnitureResponse;
import com.itta.toggleme.room.dto.RoomFurnitureUpdateRequest;
import com.itta.toggleme.room.dto.RoomResponse;
import com.itta.toggleme.room.dto.RoomVisibilityUpdateRequest;
import com.itta.toggleme.room.dto.RoomTagResponse;
import com.itta.toggleme.room.service.RoomService;
import com.itta.toggleme.room.service.RoomTagService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomTagService roomTagService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getMyRooms(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(roomService.getMyRooms(memberId(jwt)));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getMyRoom(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getMyRoom(memberId(jwt), roomId));
    }

    // 가구별 대표 태그. 규칙은 RoomTagService
    @GetMapping("/{roomId}/tags")
    public ResponseEntity<List<RoomTagResponse>> getTags(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomTagService.getMyRoomTags(memberId(jwt), roomId));
    }

    @GetMapping("/{roomId}/floors")
    public ResponseEntity<List<RoomFloorResponse>> getFloors(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(roomService.getFloors(memberId(jwt), roomId));
    }

    @GetMapping("/{roomId}/floors/{floorId}/furniture")
    public ResponseEntity<List<RoomFurnitureResponse>> getFurniture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @PathVariable Long floorId
    ) {
        return ResponseEntity.ok(roomService.getFurniture(memberId(jwt), roomId, floorId));
    }

    @PutMapping("/{roomId}/floors/{floorId}/furniture")
    public ResponseEntity<List<RoomFurnitureResponse>> updateFurniture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @PathVariable Long floorId,
            @Valid @RequestBody RoomFurnitureUpdateRequest request
    ) {
        return ResponseEntity.ok(roomService.updateFurniture(memberId(jwt), roomId, floorId, request));
    }

    @PatchMapping("/{roomId}/visibility")
    public ResponseEntity<RoomResponse> changeVisibility(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomVisibilityUpdateRequest request
    ) {
        return ResponseEntity.ok(roomService.changeVisibility(memberId(jwt), roomId, request.visibility()));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

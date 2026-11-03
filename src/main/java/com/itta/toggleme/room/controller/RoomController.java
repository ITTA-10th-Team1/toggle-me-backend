package com.itta.toggleme.room.controller;

import com.itta.toggleme.room.dto.RoomFloorResponse;
import com.itta.toggleme.room.dto.RoomFurnitureResponse;
import com.itta.toggleme.room.dto.RoomFurnitureUpdateRequest;
import com.itta.toggleme.room.dto.RoomResponse;
import com.itta.toggleme.room.dto.RoomVisibilityUpdateRequest;
import com.itta.toggleme.room.dto.RoomTagResponse;
import com.itta.toggleme.room.service.RoomService;
import com.itta.toggleme.room.service.RoomTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "방", description = "내 방, 층, 가구 배치, 방 태그, 공개 설정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomTagService roomTagService;

    @Operation(summary = "내 방 목록 조회")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getMyRooms(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(roomService.getMyRooms(memberId(jwt)));
    }

    @Operation(summary = "내 방 상세 조회")
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getMyRoom(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getMyRoom(memberId(jwt), roomId));
    }

    // 가구별 대표 태그. 규칙은 RoomTagService
    @Operation(summary = "방 태그 조회 (가구별 대표 태그, 공개 아이템만)")
    @GetMapping("/{roomId}/tags")
    public ResponseEntity<List<RoomTagResponse>> getTags(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return ResponseEntity.ok(roomTagService.getMyRoomTags(memberId(jwt), roomId));
    }

    @Operation(summary = "방 층 목록 조회")
    @GetMapping("/{roomId}/floors")
    public ResponseEntity<List<RoomFloorResponse>> getFloors(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(roomService.getFloors(memberId(jwt), roomId));
    }

    @Operation(summary = "방 가구 조회 (3D 배치, 숨김 포함)")
    @GetMapping("/{roomId}/floors/{floorId}/furniture")
    public ResponseEntity<List<RoomFurnitureResponse>> getFurniture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @PathVariable Long floorId
    ) {
        return ResponseEntity.ok(roomService.getFurniture(memberId(jwt), roomId, floorId));
    }

    @Operation(summary = "방 가구 커스터마이징 저장 (디자인·숨김)")
    @PutMapping("/{roomId}/floors/{floorId}/furniture")
    public ResponseEntity<List<RoomFurnitureResponse>> updateFurniture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long roomId,
            @PathVariable Long floorId,
            @Valid @RequestBody RoomFurnitureUpdateRequest request
    ) {
        return ResponseEntity.ok(roomService.updateFurniture(memberId(jwt), roomId, floorId, request));
    }

    @Operation(summary = "방 공개 여부 변경")
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

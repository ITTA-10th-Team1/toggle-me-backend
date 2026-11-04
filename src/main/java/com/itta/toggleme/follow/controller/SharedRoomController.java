package com.itta.toggleme.follow.controller;

import com.itta.toggleme.follow.service.SharedRoomService;
import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.room.dto.SharedRoomResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/{handle}/room")
public class SharedRoomController {

    private final SharedRoomService sharedRoomService;

    @GetMapping
    public ResponseEntity<SharedRoomResponse> getRoom(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle) {
        return ResponseEntity.ok(sharedRoomService.getRoom(memberId(jwt), handle));
    }

    // 20개씩. 다음 장은 받은 목록의 마지막 id를 before로 넘긴다
    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getItems(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String handle,
            @RequestParam Long furnitureTypeId,
            @RequestParam(required = false) Long before
    ) {
        return ResponseEntity.ok(sharedRoomService.getItems(memberId(jwt), handle, furnitureTypeId, before));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

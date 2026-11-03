package com.itta.toggleme.room.controller;

import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.dto.RoomStyleResponse;
import com.itta.toggleme.room.service.RoomStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "방 스타일", description = "벽·바닥 스타일 카탈로그")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-styles")
public class RoomStyleController {

    private final RoomStyleService roomStyleService;

    @Operation(summary = "방 스타일 목록 조회 (WALL·FLOOR)")
    @GetMapping
    public ResponseEntity<List<RoomStyleResponse>> getStyles(@RequestParam RoomStyleType type) {
        return ResponseEntity.ok(roomStyleService.getStyles(type));
    }
}

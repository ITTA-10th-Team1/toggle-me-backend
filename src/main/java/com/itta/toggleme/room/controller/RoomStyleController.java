package com.itta.toggleme.room.controller;

import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.dto.RoomStyleResponse;
import com.itta.toggleme.room.service.RoomStyleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-styles")
public class RoomStyleController {

    private final RoomStyleService roomStyleService;

    @GetMapping
    public ResponseEntity<List<RoomStyleResponse>> getStyles(@RequestParam RoomStyleType type) {
        return ResponseEntity.ok(roomStyleService.getStyles(type));
    }
}

package com.itta.toggleme.item.controller;

import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "아이템", description = "기록하기 ③ 모든 가구 공통 기록 저장")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "③ 아이템 기록 저장 (모든 가구 공통, 태그 0~3개)")
    @PostMapping
    public ResponseEntity<ItemResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.create(Long.valueOf(jwt.getSubject()), request));
    }
}

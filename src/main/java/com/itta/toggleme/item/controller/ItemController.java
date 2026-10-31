package com.itta.toggleme.item.controller;

import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.item.service.ItemService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.create(Long.valueOf(jwt.getSubject()), request));
    }
}

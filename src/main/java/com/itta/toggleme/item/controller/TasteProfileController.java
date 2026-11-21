package com.itta.toggleme.item.controller;

import com.itta.toggleme.item.dto.TasteProfileResponse;
import com.itta.toggleme.item.service.TasteProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "취향 카드", description = "내가 저장한 기록의 태그로 보는 나")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me/taste-profile")
public class TasteProfileController {

    private final TasteProfileService tasteProfileService;

    @Operation(summary = "취향 카드 조회 (최근 30일 공개 기록의 태그, 기록 5개 미만이면 eligible=false)")
    @GetMapping
    public ResponseEntity<TasteProfileResponse> getTasteProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(tasteProfileService.getProfile(Long.valueOf(jwt.getSubject())));
    }
}

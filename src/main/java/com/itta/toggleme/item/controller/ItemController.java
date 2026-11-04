package com.itta.toggleme.item.controller;

import com.itta.toggleme.item.dto.ItemCalendarDayResponse;
import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.dto.ItemResponse;
import com.itta.toggleme.item.dto.ItemUpdateRequest;
import com.itta.toggleme.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "아이템", description = "모든 가구 공통 기록 등록·조회·수정·삭제")
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
                .body(itemService.create(memberId(jwt), request));
    }

    @Operation(summary = "가구별 하루 기록 조회 (date=YYYY-MM-DD)")
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getByDate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long furnitureTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(itemService.getByDate(memberId(jwt), furnitureTypeId, date));
    }

    @Operation(summary = "가구별 월간 달력 (기록이 있는 날짜와 개수, month=YYYY-MM)")
    @GetMapping("/calendar")
    public ResponseEntity<List<ItemCalendarDayResponse>> getCalendar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long furnitureTypeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(itemService.getCalendar(memberId(jwt), furnitureTypeId, month));
    }

    @Operation(summary = "기록 수정 (보낸 값만 바꿈, 메모는 빈 문자열이면 삭제)")
    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long itemId,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        return ResponseEntity.ok(itemService.update(memberId(jwt), itemId, request));
    }

    @Operation(summary = "기록 삭제 (S3 원본도 삭제)")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        itemService.delete(memberId(jwt), itemId);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

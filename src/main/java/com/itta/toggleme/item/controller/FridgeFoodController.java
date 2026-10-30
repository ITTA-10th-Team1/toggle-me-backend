package com.itta.toggleme.item.controller;

import com.itta.toggleme.item.dto.FridgeCalendarDayResponse;
import com.itta.toggleme.item.dto.FridgeFoodCreateRequest;
import com.itta.toggleme.item.dto.FridgeFoodResponse;
import com.itta.toggleme.item.dto.FridgeFoodUpdateRequest;
import com.itta.toggleme.item.service.FridgeFoodService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fridge/foods")
public class FridgeFoodController {

    private final FridgeFoodService fridgeFoodService;

    @PostMapping
    public ResponseEntity<FridgeFoodResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FridgeFoodCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fridgeFoodService.create(memberId(jwt), request));
    }

    @GetMapping
    public ResponseEntity<List<FridgeFoodResponse>> getByDate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fridgeFoodService.getByDate(memberId(jwt), date));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<FridgeCalendarDayResponse>> getCalendar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(fridgeFoodService.getCalendar(memberId(jwt), month));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<FridgeFoodResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long itemId,
            @Valid @RequestBody FridgeFoodUpdateRequest request
    ) {
        return ResponseEntity.ok(fridgeFoodService.update(memberId(jwt), itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        fridgeFoodService.delete(memberId(jwt), itemId);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}

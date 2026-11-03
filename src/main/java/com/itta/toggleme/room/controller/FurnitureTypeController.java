package com.itta.toggleme.room.controller;

import com.itta.toggleme.room.dto.FurnitureTypeResponse;
import com.itta.toggleme.room.dto.FurnitureVariantResponse;
import com.itta.toggleme.room.service.FurnitureTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가구 카탈로그", description = "가구 종류와 디자인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/furniture-types")
public class FurnitureTypeController {

    private final FurnitureTypeService furnitureTypeService;

    @Operation(summary = "가구 종류 목록 조회")
    @GetMapping
    public ResponseEntity<List<FurnitureTypeResponse>> getFurnitureTypes() {
        return ResponseEntity.ok(furnitureTypeService.getFurnitureTypes());
    }

    @Operation(summary = "가구 디자인 목록 조회")
    @GetMapping("/{furnitureTypeId}/variants")
    public ResponseEntity<List<FurnitureVariantResponse>> getVariants(@PathVariable Long furnitureTypeId) {
        return ResponseEntity.ok(furnitureTypeService.getVariants(furnitureTypeId));
    }
}

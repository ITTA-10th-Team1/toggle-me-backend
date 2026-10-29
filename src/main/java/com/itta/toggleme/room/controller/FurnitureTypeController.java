package com.itta.toggleme.room.controller;

import com.itta.toggleme.room.dto.FurnitureTypeResponse;
import com.itta.toggleme.room.dto.FurnitureVariantResponse;
import com.itta.toggleme.room.service.FurnitureTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/furniture-types")
public class FurnitureTypeController {

    private final FurnitureTypeService furnitureTypeService;

    @GetMapping
    public ResponseEntity<List<FurnitureTypeResponse>> getFurnitureTypes() {
        return ResponseEntity.ok(furnitureTypeService.getFurnitureTypes());
    }

    @GetMapping("/{furnitureTypeId}/variants")
    public ResponseEntity<List<FurnitureVariantResponse>> getVariants(@PathVariable Long furnitureTypeId) {
        return ResponseEntity.ok(furnitureTypeService.getVariants(furnitureTypeId));
    }
}

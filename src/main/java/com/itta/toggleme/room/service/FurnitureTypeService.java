package com.itta.toggleme.room.service;

import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.dto.FurnitureTypeResponse;
import com.itta.toggleme.room.dto.FurnitureVariantResponse;
import com.itta.toggleme.room.exception.FurnitureTypeNotFoundException;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FurnitureTypeService {

    private final FurnitureTypeRepository furnitureTypeRepository;
    private final FurnitureVariantRepository furnitureVariantRepository;

    public List<FurnitureTypeResponse> getFurnitureTypes() {
        return furnitureTypeRepository.findAllByStatusOrderByDisplayOrder(CatalogStatus.ACTIVE).stream()
                .map(FurnitureTypeResponse::from)
                .toList();
    }

    public List<FurnitureVariantResponse> getVariants(Long furnitureTypeId) {
        FurnitureType furnitureType = furnitureTypeRepository.findByIdAndStatus(furnitureTypeId, CatalogStatus.ACTIVE)
                .orElseThrow(FurnitureTypeNotFoundException::new);
        return furnitureVariantRepository
                .findAllByFurnitureTypeIdAndStatusOrderById(furnitureType.getId(), CatalogStatus.ACTIVE).stream()
                .map(FurnitureVariantResponse::from)
                .toList();
    }
}

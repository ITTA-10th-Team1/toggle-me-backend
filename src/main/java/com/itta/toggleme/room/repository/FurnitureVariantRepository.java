package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.FurnitureVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FurnitureVariantRepository extends JpaRepository<FurnitureVariant, Long> {

    List<FurnitureVariant> findAllByFurnitureTypeIdAndStatusOrderById(Long furnitureTypeId, CatalogStatus status);

    Optional<FurnitureVariant> findFirstByFurnitureTypeIdAndStatusOrderById(
            Long furnitureTypeId,
            CatalogStatus status
    );

    List<FurnitureVariant> findAllByIdInAndStatus(List<Long> ids, CatalogStatus status);
}

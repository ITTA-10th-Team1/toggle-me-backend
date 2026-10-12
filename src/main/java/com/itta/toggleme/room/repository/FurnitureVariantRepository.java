package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.FurnitureVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FurnitureVariantRepository extends JpaRepository<FurnitureVariant, Long> {

    List<FurnitureVariant> findAllByFurnitureTypeIdAndStatusOrderById(Long furnitureTypeId, CatalogStatus status);
}

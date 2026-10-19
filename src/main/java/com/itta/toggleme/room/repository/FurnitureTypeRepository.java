package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.FurnitureType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FurnitureTypeRepository extends JpaRepository<FurnitureType, Long> {

    List<FurnitureType> findAllByStatusOrderByDisplayOrder(CatalogStatus status);

    Optional<FurnitureType> findByIdAndStatus(Long id, CatalogStatus status);
}

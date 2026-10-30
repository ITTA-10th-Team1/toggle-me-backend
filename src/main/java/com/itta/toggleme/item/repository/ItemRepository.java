package com.itta.toggleme.item.repository;

import com.itta.toggleme.item.domain.Item;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    long countByMemberIdAndFurnitureTypeIdAndRecordedOn(Long memberId, Long furnitureTypeId, LocalDate recordedOn);

    List<Item> findAllByMemberIdAndFurnitureTypeIdAndRecordedOnOrderById(
            Long memberId,
            Long furnitureTypeId,
            LocalDate recordedOn
    );

    List<Item> findAllByMemberIdAndFurnitureTypeIdAndRecordedOnBetweenOrderByRecordedOnAscIdAsc(
            Long memberId,
            Long furnitureTypeId,
            LocalDate from,
            LocalDate to
    );

    Optional<Item> findByIdAndMemberIdAndFurnitureTypeId(Long id, Long memberId, Long furnitureTypeId);

    boolean existsByImageKey(String imageKey);
}

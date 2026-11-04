package com.itta.toggleme.item.repository;

import com.itta.toggleme.item.domain.Item;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

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

    Optional<Item> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByImageKey(String imageKey);

    long countByMemberIdAndFurnitureTypeIdAndRecordedOn(Long memberId, Long furnitureTypeId, LocalDate recordedOn);

    // 가구별 기록 목록, 최근에 올린 순. before가 있으면 그 id보다 앞선 기록부터
    @Query("""
            select item from Item item
            where item.member.id = :ownerId and item.furnitureType.id = :furnitureTypeId
              and (:includePrivate = true or item.visibility = 'PUBLIC')
              and (:before is null or item.id < :before)
            order by item.id desc
            """)
    List<Item> findRoomItems(
            @Param("ownerId") Long ownerId,
            @Param("furnitureTypeId") Long furnitureTypeId,
            @Param("includePrivate") boolean includePrivate,
            @Param("before") Long before,
            Limit limit
    );
}

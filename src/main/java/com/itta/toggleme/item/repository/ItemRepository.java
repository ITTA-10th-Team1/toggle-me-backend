package com.itta.toggleme.item.repository;

import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemVisibility;
import com.itta.toggleme.member.domain.MemberStatus;
import java.time.Instant;
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

    long countByMemberIdAndVisibilityAndRecordedOnBetween(
            Long memberId, ItemVisibility visibility, LocalDate from, LocalDate to);

    long countByMemberIdAndFurnitureTypeIdAndRecordedOn(Long memberId, Long furnitureTypeId, LocalDate recordedOn);

    @Query("select item.createdAt from Item item where item.createdAt >= :from")
    List<Instant> findCreatedAtSince(@Param("from") Instant from);

    // 기간 안에 가입한 회원마다 기록한 서로 다른 날짜 수. 기록이 없는 회원은 행이 없다
    @Query("""
            select count(distinct item.recordedOn) from Item item
            where item.member.createdAt >= :from and item.member.status = :status
            group by item.member.id
            """)
    List<Long> countRecordedDaysPerMemberJoinedSince(@Param("from") Instant from, @Param("status") MemberStatus status);

    // 사용 중인 가구마다 기간 안의 기록 수. 기록이 없는 가구도 0으로 나온다
    @Query("""
            select type.code as code, type.name as name, count(item) as items
            from FurnitureType type
            left join Item item on item.furnitureType = type and item.createdAt >= :from
            where type.status = com.itta.toggleme.room.domain.CatalogStatus.ACTIVE
            group by type.id, type.code, type.name, type.displayOrder
            order by count(item) desc, type.displayOrder
            """)
    List<FurnitureItemCount> countByFurnitureTypeSince(@Param("from") Instant from);

    interface FurnitureItemCount {
        String getCode();

        String getName();

        long getItems();
    }

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

    // 내 가구별 기록, 오래된 순. cursor보다 큰 id부터
    @Query("""
            select item from Item item
            where item.member.id = :memberId and item.furnitureType.id = :furnitureTypeId
              and (:cursor is null or item.id > :cursor)
            order by item.id asc
            """)
    List<Item> findMyItemsByFurnitureOldest(
            @Param("memberId") Long memberId,
            @Param("furnitureTypeId") Long furnitureTypeId,
            @Param("cursor") Long cursor,
            Limit limit
    );

    // 내 기록 전체(모든 가구·모든 날짜). 최신순은 cursor보다 작은 id부터, 오래된 순은 큰 id부터
    @Query("""
            select item from Item item
            where item.member.id = :memberId and (:cursor is null or item.id < :cursor)
            order by item.id desc
            """)
    List<Item> findMyItemsLatest(@Param("memberId") Long memberId, @Param("cursor") Long cursor, Limit limit);

    @Query("""
            select item from Item item
            where item.member.id = :memberId and (:cursor is null or item.id > :cursor)
            order by item.id asc
            """)
    List<Item> findMyItemsOldest(@Param("memberId") Long memberId, @Param("cursor") Long cursor, Limit limit);
}

package com.itta.toggleme.item.repository;

import com.itta.toggleme.item.domain.ItemTag;
import com.itta.toggleme.item.domain.ItemVisibility;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemTagRepository extends JpaRepository<ItemTag, Long> {

    List<ItemTag> findAllByItemIdOrderByPosition(Long itemId);

    List<ItemTag> findAllByItemIdInOrderByPosition(Collection<Long> itemIds);

    // 방 주인의 아이템 중 공개 상태이고, 방에 켜져 있는 가구에 기록한 것의 태그만 가져온다
    @Query("""
            select new com.itta.toggleme.item.repository.RoomTagCandidate(
                item.furnitureType.id, tag.name, tag.position, item.id, item.createdAt)
            from ItemTag tag join tag.item item
            where item.member.id = :ownerId
              and item.visibility = :visibility
              and exists (
                  select 1 from RoomFurniture furniture
                  where furniture.floor.room.id = :roomId
                    and furniture.active = true
                    and furniture.variant.furnitureType = item.furnitureType)
            """)
    List<RoomTagCandidate> findRoomTagCandidates(
            @Param("roomId") Long roomId,
            @Param("ownerId") Long ownerId,
            @Param("visibility") ItemVisibility visibility
    );

    // 취향 카드: 기간 안의 공개 기록에 붙은 태그를 가구 구분 없이 합쳐 센다. 많이 나온 순, 동점이면 이름 순
    // 한 기록의 태그 이름은 서로 달라서(저장할 때 중복 제거) 태그 행 수가 곧 기록 수다
    @Query("""
            select new com.itta.toggleme.item.repository.TasteTagCount(tag.name, count(tag))
            from ItemTag tag join tag.item item
            where item.member.id = :memberId
              and item.visibility = :visibility
              and item.recordedOn between :from and :to
            group by tag.name
            order by count(tag) desc, tag.name asc
            """)
    List<TasteTagCount> findTasteTagCounts(
            @Param("memberId") Long memberId,
            @Param("visibility") ItemVisibility visibility,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );
}

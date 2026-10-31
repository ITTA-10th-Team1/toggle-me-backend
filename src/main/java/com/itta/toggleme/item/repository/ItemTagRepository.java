package com.itta.toggleme.item.repository;

import com.itta.toggleme.item.domain.ItemTag;
import com.itta.toggleme.item.domain.ItemVisibility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemTagRepository extends JpaRepository<ItemTag, Long> {

    List<ItemTag> findAllByItemIdOrderByPosition(Long itemId);

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
}

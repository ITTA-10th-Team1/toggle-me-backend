package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.RoomFurniture;
import com.itta.toggleme.room.domain.RoomStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomFurnitureRepository extends JpaRepository<RoomFurniture, Long> {

    List<RoomFurniture> findAllByFloorId(Long floorId);

    @Query("""
            select count(furniture) > 0 from RoomFurniture furniture
            where furniture.floor.room.owner.id = :ownerId
              and furniture.floor.room.status = :roomStatus
              and furniture.active = true
              and furniture.variant.furnitureType.id = :furnitureTypeId
            """)
    boolean existsActiveByOwnerIdAndFurnitureTypeId(
            @Param("ownerId") Long ownerId,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("furnitureTypeId") Long furnitureTypeId
    );
}

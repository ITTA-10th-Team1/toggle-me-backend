package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.RoomFurniture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomFurnitureRepository extends JpaRepository<RoomFurniture, Long> {

    List<RoomFurniture> findAllByFloorIdAndActiveTrueOrderByRenderOrder(Long floorId);
}

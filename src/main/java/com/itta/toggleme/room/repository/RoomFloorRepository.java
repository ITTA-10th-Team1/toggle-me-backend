package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.RoomFloor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomFloorRepository extends JpaRepository<RoomFloor, Long> {

    List<RoomFloor> findAllByRoomIdOrderByFloorNumber(Long roomId);
}

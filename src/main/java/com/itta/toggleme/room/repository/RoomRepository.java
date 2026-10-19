package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByOwnerIdOrderById(Long ownerId);

    Optional<Room> findByIdAndOwnerId(Long id, Long ownerId);

    List<Room> findAllByOwnerIdAndStatusOrderById(Long ownerId, RoomStatus status);

    Optional<Room> findByIdAndOwnerIdAndStatus(Long id, Long ownerId, RoomStatus status);

    Optional<Room> findFirstByOwnerIdOrderById(Long ownerId);
}

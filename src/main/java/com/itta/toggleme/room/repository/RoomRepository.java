package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByOwnerIdOrderById(Long ownerId);

    Optional<Room> findByIdAndOwnerId(Long id, Long ownerId);

    List<Room> findAllByOwnerIdAndStatusOrderById(Long ownerId, RoomStatus status);

    Optional<Room> findByIdAndOwnerIdAndStatus(Long id, Long ownerId, RoomStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select room from Room room
            where room.id = :id and room.owner.id = :ownerId and room.status = :status
            """)
    Optional<Room> findForUpdateByIdAndOwnerIdAndStatus(
            @Param("id") Long id,
            @Param("ownerId") Long ownerId,
            @Param("status") RoomStatus status
    );

    Optional<Room> findFirstByOwnerIdOrderById(Long ownerId);
}

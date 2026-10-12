package com.itta.toggleme.room.repository;

import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomStyleRepository extends JpaRepository<RoomStyle, Long> {

    List<RoomStyle> findAllByTypeAndStatusOrderById(RoomStyleType type, CatalogStatus status);
}

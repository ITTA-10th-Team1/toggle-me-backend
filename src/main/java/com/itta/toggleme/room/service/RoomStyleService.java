package com.itta.toggleme.room.service;

import com.itta.toggleme.room.domain.CatalogStatus;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.dto.RoomStyleResponse;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomStyleService {

    private final RoomStyleRepository roomStyleRepository;

    public List<RoomStyleResponse> getStyles(RoomStyleType type) {
        return roomStyleRepository.findAllByTypeAndStatusOrderById(type, CatalogStatus.ACTIVE).stream()
                .map(RoomStyleResponse::from)
                .toList();
    }
}

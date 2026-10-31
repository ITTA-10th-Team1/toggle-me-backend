package com.itta.toggleme.item.repository;

import com.itta.toggleme.item.domain.ItemTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemTagRepository extends JpaRepository<ItemTag, Long> {

    List<ItemTag> findAllByItemIdOrderByPosition(Long itemId);
}

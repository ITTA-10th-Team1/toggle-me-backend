package com.itta.toggleme.item.dto;

import com.itta.toggleme.item.repository.TasteTagCount;

public record TasteTagResponse(String name, long count) {

    public static TasteTagResponse from(TasteTagCount tag) {
        return new TasteTagResponse(tag.name(), tag.count());
    }
}

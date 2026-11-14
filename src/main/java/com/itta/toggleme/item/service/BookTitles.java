package com.itta.toggleme.item.service;

import java.util.List;
import java.util.Objects;

// AI가 돌려준 비슷한 책 제목. 모델 출력이 규칙을 넘길 때가 있어 서버가 한 번 더 정리한다
public record BookTitles(List<String> books) {

    private static final int MAX_BOOKS = 5;
    private static final int MAX_TITLE_LENGTH = 40;

    public BookTitles {
        books = books == null ? List.of() : books.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .map(title -> title.startsWith("#") ? title.substring(1).strip() : title)
                .filter(title -> !title.isEmpty() && title.length() <= MAX_TITLE_LENGTH)
                .distinct()
                .limit(MAX_BOOKS)
                .toList();
    }
}

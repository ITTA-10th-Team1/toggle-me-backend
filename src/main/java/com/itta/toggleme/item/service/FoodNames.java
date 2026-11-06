package com.itta.toggleme.item.service;

import java.util.List;
import java.util.Objects;

// AI가 돌려준 비슷한 음식 이름. 모델 출력이 규칙을 넘길 때가 있어 서버가 한 번 더 정리한다
public record FoodNames(List<String> foods) {

    private static final int MAX_FOODS = 3;
    private static final int MAX_NAME_LENGTH = 20;

    public FoodNames {
        foods = foods == null ? List.of() : foods.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .map(name -> name.startsWith("#") ? name.substring(1).strip() : name)
                .filter(name -> !name.isEmpty() && name.length() <= MAX_NAME_LENGTH)
                .distinct()
                .limit(MAX_FOODS)
                .toList();
    }
}

package com.itta.toggleme.image.service;

import java.util.Map;
import java.util.Optional;

/**
 * 가구별로 AI 태그를 뽑을 때 공통 규칙 뒤에 덧붙이는 지침. 키는 가구 code(소문자)다.
 * 지침이 없는 가구는 공통 규칙만 쓴다. 새 가구를 더하면 한 줄을 추가한다.
 */
public final class FurnitureTagGuide {

    private static final Map<String, String> GUIDES = Map.of(
            "fridge", "냉장고에 기록하는 음식 사진이다. 사진 속 음식에 집중해 메뉴, 주재료, 맛이나 식사 분위기를 태그로 만든다.",
            "mirror", "거울에 기록하는 착장 사진이다. 입은 옷에 집중해 옷의 종류, 색, 스타일, 어울리는 상황을 태그로 만든다."
    );

    private FurnitureTagGuide() {
    }

    public static Optional<String> forCode(String furnitureCode) {
        return Optional.ofNullable(furnitureCode).map(GUIDES::get);
    }
}

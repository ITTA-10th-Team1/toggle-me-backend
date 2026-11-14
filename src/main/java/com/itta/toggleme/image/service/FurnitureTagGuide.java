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
            "mirror", "거울에 기록하는 착장 사진이다. 입은 옷에 집중해 옷의 종류, 색, 스타일, 어울리는 상황을 태그로 만든다.\n"
                    + "또한 이 착장과 비슷한 옷을 쇼핑몰에서 찾을 검색어를 searchQueries에 2~3개 만든다. "
                    + "계절, 색, 핏, 옷 종류 순서의 짧은 한국어로 쓰고(예: 가을 베이지 오버핏 트렌치코트), "
                    + "각 검색어는 20자 이내, 브랜드명과 #은 쓰지 않는다.",
            "bookshelf", "책장에 기록하는 책 사진이다. 사진 속 책에 집중해 책 제목(표지나 책등에 보이면), 장르, 분위기를 태그로 만든다. "
                    + "제목이 보이지 않으면 추측하지 말고 장르와 분위기만 태그로 만든다."
    );

    private FurnitureTagGuide() {
    }

    // 쇼핑몰 검색어(searchQueries)는 거울에서만 만든다
    public static boolean createsSearchQueries(String furnitureCode) {
        return "mirror".equals(furnitureCode);
    }

    public static Optional<String> forCode(String furnitureCode) {
        return Optional.ofNullable(furnitureCode).map(GUIDES::get);
    }
}

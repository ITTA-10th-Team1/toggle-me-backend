package com.itta.toggleme.item.service;

import java.util.Map;
import java.util.OptionalInt;

/**
 * 가구별 하루 등록 개수 제한. 키는 가구 code(소문자)이고, 표에 없는 가구는 제한이 없다(PRD ARCH-02).
 * 제한을 바꾸거나 다른 가구에 걸려면 이 표만 고친다.
 */
public final class ItemDailyLimit {

    // 냉장고·거울·책장 하루 10장(처음엔 1장이었으나 테스트·사용성을 위해 늘림. 추후 조정)
    private static final Map<String, Integer> LIMITS = Map.of("fridge", 10, "mirror", 10, "bookshelf", 10);

    private ItemDailyLimit() {
    }

    public static OptionalInt of(String furnitureCode) {
        // Map.of는 get(null)에서 예외를 던진다
        Integer limit = furnitureCode == null ? null : LIMITS.get(furnitureCode);
        return limit == null ? OptionalInt.empty() : OptionalInt.of(limit);
    }
}

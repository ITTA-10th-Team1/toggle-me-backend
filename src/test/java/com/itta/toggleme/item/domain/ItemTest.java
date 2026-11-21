package com.itta.toggleme.item.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ItemTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    private static Item record(String memo) {
        return Item.record(null, null, TODAY, "images/test.jpg", memo, ItemVisibility.PRIVATE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\n\t"})
    void 메모가_없거나_공백뿐이면_null로_저장한다(String memo) {
        assertThat(record(memo).getMemo()).isNull();
    }

    @Test
    void 내용이_있는_메모는_공백을_포함해_그대로_저장한다() {
        assertThat(record("  흥미진진한 전개  ").getMemo()).isEqualTo("  흥미진진한 전개  ");
    }

    @Test
    void 수정할_때_빈_메모를_보내면_지우고_보내지_않으면_유지한다() {
        Item item = record("원래 메모");

        item.update(null, null);
        assertThat(item.getMemo()).isEqualTo("원래 메모");

        item.update("새 메모", null);
        assertThat(item.getMemo()).isEqualTo("새 메모");

        item.update("  ", null);
        assertThat(item.getMemo()).isNull();
    }
}

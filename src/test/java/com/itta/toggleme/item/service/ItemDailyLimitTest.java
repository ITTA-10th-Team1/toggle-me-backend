package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemDailyLimitTest {

    @Test
    void 냉장고와_거울은_하루_1장이다() {
        assertThat(ItemDailyLimit.of("fridge")).hasValue(1);
        assertThat(ItemDailyLimit.of("mirror")).hasValue(1);
    }

    @Test
    void 표에_없는_가구는_제한이_없다() {
        assertThat(ItemDailyLimit.of("desk")).isEmpty();
        assertThat(ItemDailyLimit.of(null)).isEmpty();
    }
}

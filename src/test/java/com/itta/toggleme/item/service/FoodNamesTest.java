package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FoodNamesTest {

    @Test
    void 빈값_중복_긴_이름을_빼고_샵을_떼서_최대_3개만_남긴다() {
        FoodNames names = new FoodNames(Arrays.asList(
                " 까르보나라 ", "#토마토 파스타", null, " ", "까르보나라", "이름이스무자를훌쩍넘어가는아주아주긴음식이름입니다",
                "리조또", "뇨끼"));

        assertThat(names.foods()).containsExactly("까르보나라", "토마토 파스타", "리조또");
    }

    @Test
    void 없으면_빈_목록이다() {
        assertThat(new FoodNames(null).foods()).isEmpty();
    }
}

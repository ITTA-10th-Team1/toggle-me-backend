package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BookTitlesTest {

    @Test
    void 빈값_중복_긴_제목을_빼고_샵을_떼서_최대_5권만_남긴다() {
        BookTitles titles = new BookTitles(Arrays.asList(
                " 여행의 이유 ", "#아몬드", null, " ", "여행의 이유", "가".repeat(41),
                "채식주의자", "소년이 온다", "불편한 편의점", "달러구트 꿈 백화점"));

        assertThat(titles.books()).containsExactly("여행의 이유", "아몬드", "채식주의자", "소년이 온다", "불편한 편의점");
    }

    @Test
    void 없으면_빈_목록이다() {
        assertThat(new BookTitles(null).books()).isEmpty();
    }
}

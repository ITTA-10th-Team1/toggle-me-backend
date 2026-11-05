package com.itta.toggleme.image.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImageAnalysisResultTest {

    @Test
    void 태그는_샵을_붙이고_빈값_중복_긴_태그를_빼서_최대_3개만_남긴다() {
        ImageAnalysisResult result = new ImageAnalysisResult(Arrays.asList(
                " 추리 ", "#추리", null, "", "#", "#스무자를훌쩍넘는아주아주아주아주긴태그입니다", "#외국소설", "#히가시노게이고", "#넷째"), null);

        assertThat(result.tags()).containsExactly("#추리", "#외국소설", "#히가시노게이고");
    }

    @Test
    void 태그가_없으면_빈_목록이다() {
        assertThat(new ImageAnalysisResult(null, null).tags()).isEqualTo(List.of());
    }

    @Test
    void 검색어는_빈값_중복_긴_검색어를_빼서_최대_3개만_남긴다() {
        ImageAnalysisResult result = new ImageAnalysisResult(List.of(), Arrays.asList(
                " 가을 베이지 오버핏 트렌치코트 ", "가을 베이지 오버핏 트렌치코트", null, " ",
                "가을 아주아주아주아주아주 긴 검색어 스물한 글자", "검정 와이드 슬랙스", "여름 흰색 린넨 셔츠", "넷째 검색어"));

        assertThat(result.searchQueries()).containsExactly("가을 베이지 오버핏 트렌치코트", "검정 와이드 슬랙스", "여름 흰색 린넨 셔츠");
    }

    @Test
    void 검색어가_없으면_빈_목록이다() {
        assertThat(new ImageAnalysisResult(List.of("#추리"), null).searchQueries()).isEmpty();
    }
}

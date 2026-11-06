package com.itta.toggleme.image.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

// image_analysis.result(JSON)에 저장하고 API로도 그대로 내보낸다.
// searchQueries는 거울에서만 채워지는 쇼핑몰 검색어다(링크는 프론트가 만든다). 예전에 저장된 결과에는 없어 빈 목록이 된다
public record ImageAnalysisResult(List<String> tags, List<String> searchQueries) {

    private static final int MAX_TAGS = 3;
    private static final int MAX_TAG_LENGTH = 20;
    private static final int MAX_SEARCH_QUERIES = 3;
    private static final int MAX_SEARCH_QUERY_LENGTH = 20;

    // 모델 출력이 프롬프트 제한을 넘길 때가 있어 화면 규칙(최대 3개)에 맞춰 정리한다
    public ImageAnalysisResult {
        tags = tags == null ? List.of() : tags.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(tag -> !tag.isEmpty() && !tag.equals("#"))
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                .filter(tag -> tag.length() <= MAX_TAG_LENGTH)
                .distinct()
                .limit(MAX_TAGS)
                .toList();
        // 형식을 어긴 검색어는 고치지 않고 버린다. 전부 버려지면 빈 목록이다
        searchQueries = searchQueries == null ? List.of() : searchQueries.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(query -> !query.isEmpty() && query.length() <= MAX_SEARCH_QUERY_LENGTH)
                .distinct()
                .limit(MAX_SEARCH_QUERIES)
                .toList();
    }

    // 모델이 searchQueries를 배열이 아닌 문자열·객체로 주면 변환 예외로 분석 전체가 실패한다.
    // 검색어는 덤이라 형식이 틀리면 빈 목록으로 두고 태그는 살린다
    @JsonCreator
    static ImageAnalysisResult fromJson(
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("searchQueries") Object searchQueries
    ) {
        List<String> queries = searchQueries instanceof List<?> list
                ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                : List.of();
        return new ImageAnalysisResult(tags, queries);
    }

    public ImageAnalysisResult withoutSearchQueries() {
        return new ImageAnalysisResult(tags, List.of());
    }
}

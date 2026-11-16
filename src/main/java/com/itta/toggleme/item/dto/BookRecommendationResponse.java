package com.itta.toggleme.item.dto;

import java.util.List;

// 저장하지 않는 책 추천 한 건. 표지가 있는 책만 내려간다
public record BookRecommendationResponse(
        String title,
        List<String> authors,
        String thumbnail,
        String url
) implements RecommendationResponse {
}

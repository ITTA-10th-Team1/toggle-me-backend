package com.itta.toggleme.item.dto;

// 저장하지 않는 AI 추천 한 건. 사진이 있는 음식만 내려간다
public record FoodRecommendationResponse(
        String name,
        String imageUrl
) implements RecommendationResponse {
}

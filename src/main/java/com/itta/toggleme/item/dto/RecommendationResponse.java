package com.itta.toggleme.item.dto;

// AI 추천 한 건의 공통 타입. 가구마다 모양이 달라서(음식: 이름·사진, 책: 제목·저자·표지·링크) 구현을 나눈다
public sealed interface RecommendationResponse permits FoodRecommendationResponse, BookRecommendationResponse {
}

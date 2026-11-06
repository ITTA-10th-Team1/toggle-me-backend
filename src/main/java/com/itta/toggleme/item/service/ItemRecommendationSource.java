package com.itta.toggleme.item.service;

import java.util.List;

// AI 추천에 필요한 기록 정보(가구 code와 태그 이름). 트랜잭션 밖에서 쓸 수 있게 값만 담는다
public record ItemRecommendationSource(String furnitureCode, List<String> tags) {
}

package com.itta.toggleme.item.service;

import com.itta.toggleme.item.client.NaverImageClient;
import com.itta.toggleme.item.dto.FoodRecommendationResponse;
import com.itta.toggleme.item.exception.ItemRecommendationFailedException;
import com.itta.toggleme.item.exception.ItemRecommendationTagRequiredException;
import com.itta.toggleme.item.exception.ItemRecommendationUnsupportedException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 냉장고 기록 상세의 "AI 추천" 버튼. 기록의 태그로 AI가 비슷한 음식을 고르고, 음식마다 네이버 이미지 검색으로 사진을 붙인다.
 * 결과는 저장하지 않는다(누를 때마다 새로 만든다). 외부 호출이 길어서 DB 트랜잭션을 잡지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemRecommendationService {

    private static final String FRIDGE_CODE = "fridge";

    private final ItemService itemService;
    private final FoodRecommender foodRecommender;
    private final NaverImageClient naverImageClient;
    private final ItemRecommendationLimiter limiter;

    public List<FoodRecommendationResponse> recommend(Long memberId, Long itemId) {
        ItemRecommendationSource source = itemService.getRecommendationSource(memberId, itemId);
        if (!FRIDGE_CODE.equals(source.furnitureCode())) {
            throw new ItemRecommendationUnsupportedException();
        }
        if (source.tags().isEmpty()) {
            throw new ItemRecommendationTagRequiredException();
        }
        limiter.acquire(memberId);
        try {
            List<String> foods = foodRecommender.recommend(source.tags());
            if (foods.isEmpty()) {
                throw new IllegalStateException("empty recommendation");
            }
            return foods.stream()
                    .map(name -> naverImageClient.findImageUrl(name)
                            .map(url -> new FoodRecommendationResponse(name, url)))
                    .flatMap(Optional::stream)
                    .toList();
        } catch (RuntimeException e) {
            // 우리 쪽 실패로 횟수를 잃지 않게 돌려준다
            limiter.release(memberId);
            log.warn("Food recommendation failed: {}", e.getClass().getSimpleName());
            throw new ItemRecommendationFailedException();
        }
    }
}

package com.itta.toggleme.item.service;

import com.itta.toggleme.item.client.KakaoBookClient;
import com.itta.toggleme.item.client.KakaoBookSearchException;
import com.itta.toggleme.item.client.NaverImageClient;
import com.itta.toggleme.item.client.NaverSearchException;
import com.itta.toggleme.item.dto.BookRecommendationResponse;
import com.itta.toggleme.item.dto.FoodRecommendationResponse;
import com.itta.toggleme.item.dto.RecommendationResponse;
import com.itta.toggleme.item.exception.ItemRecommendationFailedException;
import com.itta.toggleme.item.exception.ItemRecommendationTagRequiredException;
import com.itta.toggleme.item.exception.ItemRecommendationUnsupportedException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기록 상세의 "추천" 버튼. 가구마다 방식이 다르다.
 * 냉장고는 기록의 태그로 AI가 비슷한 음식을 고르고 음식마다 네이버 이미지 검색으로 사진을 붙인다.
 * 책장은 AI 없이 태그를 검색어로 카카오 책 검색을 해서 책 목록을 준다.
 * 결과는 저장하지 않는다(누를 때마다 새로 만든다). 외부 호출이 길어서 DB 트랜잭션을 잡지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemRecommendationService {

    private static final String FRIDGE_CODE = "fridge";
    private static final String BOOKSHELF_CODE = "bookshelf";
    // 책 검색은 앞쪽 태그 3개를 검색어로 쓰고 태그마다 2권씩 받아 최대 5권을 보여 준다
    private static final int BOOK_QUERY_TAGS = 3;
    private static final int BOOKS_PER_TAG = 2;
    private static final int MAX_BOOKS = 5;

    private final ItemService itemService;
    private final FoodRecommender foodRecommender;
    private final NaverImageClient naverImageClient;
    private final KakaoBookClient kakaoBookClient;
    private final ItemRecommendationLimiter limiter;

    public List<RecommendationResponse> recommend(Long memberId, Long itemId) {
        ItemRecommendationSource source = itemService.getRecommendationSource(memberId, itemId);
        boolean fridge = FRIDGE_CODE.equals(source.furnitureCode());
        if (!fridge && !BOOKSHELF_CODE.equals(source.furnitureCode())) {
            throw new ItemRecommendationUnsupportedException();
        }
        if (source.tags().isEmpty()) {
            throw new ItemRecommendationTagRequiredException();
        }
        LocalDate acquiredOn = limiter.acquire(memberId);
        try {
            if (!fridge) {
                return List.copyOf(findBooks(source.tags()));
            }
            List<String> foods = foodRecommender.recommend(source.tags());
            if (foods.isEmpty()) {
                throw new IllegalStateException("empty recommendation");
            }
            return List.copyOf(findImages(foods));
        } catch (RuntimeException e) {
            // 우리 쪽 실패로 횟수를 잃지 않게, 요청을 시작한 날짜의 횟수를 돌려준다
            limiter.release(memberId, acquiredOn);
            log.warn("Recommendation failed: {}", e.getClass().getSimpleName());
            throw new ItemRecommendationFailedException();
        }
    }

    // 태그마다 책을 검색해 같은 책(링크 기준)은 한 번만 담는다. 일부 검색만 장애로 실패하면 나머지를 보여 주고,
    // 모든 검색이 장애로 실패하면(예: 키 누락) 빈 목록 대신 실패로 알린다. 결과가 없는 것은 장애가 아니라 빈 목록이다
    private List<BookRecommendationResponse> findBooks(List<String> tags) {
        List<String> queries = tags.stream().limit(BOOK_QUERY_TAGS).toList();
        Map<String, BookRecommendationResponse> found = new LinkedHashMap<>();
        int failures = 0;
        for (String query : queries) {
            try {
                for (BookRecommendationResponse book : kakaoBookClient.search(query, BOOKS_PER_TAG)) {
                    found.putIfAbsent(book.url() != null ? book.url() : book.title(), book);
                }
            } catch (KakaoBookSearchException e) {
                failures++;
            }
        }
        if (failures == queries.size()) {
            throw new IllegalStateException("all book searches failed");
        }
        return found.values().stream().limit(MAX_BOOKS).toList();
    }

    // 검색 결과가 없는 음식은 뺀다. 일부 검색만 장애로 실패하면 나머지를 보여 주고,
    // 모든 검색이 장애로 실패하면(예: 키 누락) 빈 목록 대신 실패로 알린다
    private List<FoodRecommendationResponse> findImages(List<String> foods) {
        List<FoodRecommendationResponse> found = new ArrayList<>();
        int failures = 0;
        for (String name : foods) {
            try {
                naverImageClient.findImageUrl(name)
                        .ifPresent(url -> found.add(new FoodRecommendationResponse(name, url)));
            } catch (NaverSearchException e) {
                failures++;
            }
        }
        if (failures == foods.size()) {
            throw new IllegalStateException("all image searches failed");
        }
        return found;
    }
}

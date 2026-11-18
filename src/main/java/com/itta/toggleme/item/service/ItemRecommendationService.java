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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기록 상세의 "추천" 버튼. 가구마다 방식이 다르다.
 * 냉장고는 기록의 태그로 AI가 비슷한 음식을 고르고 음식마다 네이버 이미지 검색으로 사진을 붙인다.
 * 책장은 AI가 비슷한 책 제목을 고르고 제목마다 카카오 책 제목 검색(동시 호출)으로 표지와 링크를 붙인다.
 * 결과는 저장하지 않는다(누를 때마다 새로 만든다). 외부 호출이 길어서 DB 트랜잭션을 잡지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemRecommendationService {

    private static final String FRIDGE_CODE = "fridge";
    private static final String BOOKSHELF_CODE = "bookshelf";
    // 책 제목마다 넉넉히 받아 그중 표지가 있는 첫 책을 쓴다(표지 없는 책을 거르고도 결과가 비지 않게)
    private static final int BOOK_SEARCH_SIZE = 5;
    private static final int MIN_TITLE_LENGTH = 2;

    private final ItemService itemService;
    private final FoodRecommender foodRecommender;
    private final BookRecommender bookRecommender;
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
        List<RecommendationResponse> result;
        try {
            if (!fridge) {
                List<String> titles = bookRecommender.recommend(source.tags());
                if (titles.isEmpty()) {
                    throw new IllegalStateException("empty recommendation");
                }
                result = List.copyOf(findBooks(titles));
            } else {
                List<String> foods = foodRecommender.recommend(source.tags());
                if (foods.isEmpty()) {
                    throw new IllegalStateException("empty recommendation");
                }
                result = List.copyOf(findImages(foods));
            }
        } catch (RuntimeException e) {
            // 장애로 실패한 경우에만 횟수를 돌려준다(요청을 시작한 날짜 기준). 결과가 비어 있는 것은 장애가 아니라서 돌려주지 않는다.
            // 비었을 때도 돌려주면 Gemini와 카카오를 횟수 제한 없이 부를 수 있다
            limiter.release(memberId, acquiredOn);
            log.warn("Recommendation failed: {}", e.getClass().getSimpleName());
            throw new ItemRecommendationFailedException();
        }
        return result;
    }

    // AI가 고른 책 제목마다 카카오 책 제목 검색 결과 중 제목이 맞는 첫 책(표지가 있는 책)을 붙이고, 같은 책(링크 기준)은 한 번만 담는다.
    // 제목마다 검색은 서로 독립이라 동시에 보내고, 결과 순서는 AI가 고른 순서를 지킨다.
    // 맞는 결과가 없는 제목(AI가 지어낸 책)은 뺀다. 일부 검색만 장애로 실패하면 나머지를 보여 주고,
    // 모든 검색이 장애로 실패하면(예: 키 누락) 빈 목록 대신 실패로 알린다
    private List<BookRecommendationResponse> findBooks(List<String> candidates) {
        // 너무 짧은 제목은 아무 책과나 맞아서 검색하지 않는다. 장애 판정은 실제로 검색한 제목만 센다
        List<String> titles = candidates.stream()
                .filter(title -> normalizeTitle(title).length() >= MIN_TITLE_LENGTH)
                .toList();
        List<Future<Optional<BookRecommendationResponse>>> searches = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String title : titles) {
                searches.add(executor.submit(() -> findBook(title)));
            }
        } // close()가 모든 검색이 끝나기를 기다린다

        Map<String, BookRecommendationResponse> found = new LinkedHashMap<>();
        int failures = 0;
        for (Future<Optional<BookRecommendationResponse>> search : searches) {
            try {
                search.get().ifPresent(book -> found.putIfAbsent(book.url() != null ? book.url() : book.title(), book));
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof KakaoBookSearchException)) {
                    throw new IllegalStateException("unexpected book search error", e.getCause());
                }
                failures++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("book search interrupted", e);
            }
        }
        if (!titles.isEmpty() && failures == titles.size()) {
            throw new IllegalStateException("all book searches failed");
        }
        return List.copyOf(found.values());
    }

    private Optional<BookRecommendationResponse> findBook(String title) {
        return kakaoBookClient.search(title, BOOK_SEARCH_SIZE).stream()
                .filter(book -> matchesTitle(book.title(), title))
                .findFirst();
    }

    // 검색은 단어만 겹쳐도 다른 책을 돌려줄 수 있어, 두 제목의 앞부분이 한쪽이 다른 쪽으로 시작하는 꼴로 일치할 때만 같은 책으로 본다.
    // 띄어쓰기·기호·대소문자 차이는 무시한다. 판본 표기("채식주의자 (스페셜 에디션)")나 AI가 붙인 부제("채식주의자 장편소설")는 같은 책이다.
    // 정규화 결과가 2자 미만인 제목은 아무 책과나 맞아서 같은 책으로 보지 않는다
    private static boolean matchesTitle(String foundTitle, String wantedTitle) {
        String found = normalizeTitle(foundTitle);
        String wanted = normalizeTitle(wantedTitle);
        if (found.length() < MIN_TITLE_LENGTH || wanted.length() < MIN_TITLE_LENGTH) {
            return false;
        }
        return found.startsWith(wanted) || wanted.startsWith(found);
    }

    private static String normalizeTitle(String title) {
        return title.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase(Locale.ROOT);
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

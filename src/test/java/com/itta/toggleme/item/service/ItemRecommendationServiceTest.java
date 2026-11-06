package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.itta.toggleme.item.client.NaverImageClient;
import com.itta.toggleme.item.client.NaverSearchException;
import com.itta.toggleme.item.dto.FoodRecommendationResponse;
import com.itta.toggleme.item.exception.ItemRecommendationFailedException;
import com.itta.toggleme.item.exception.ItemRecommendationTagRequiredException;
import com.itta.toggleme.item.exception.ItemRecommendationUnsupportedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemRecommendationServiceTest {

    private final ItemService itemService = mock(ItemService.class);
    private final FoodRecommender foodRecommender = mock(FoodRecommender.class);
    private final NaverImageClient naverImageClient = mock(NaverImageClient.class);
    private final ItemRecommendationLimiter limiter = mock(ItemRecommendationLimiter.class);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private final ItemRecommendationService service =
            new ItemRecommendationService(itemService, foodRecommender, naverImageClient, limiter);

    @BeforeEach
    void setUp() {
        given(limiter.acquire(1L)).willReturn(TODAY);
    }

    @Test
    void 사진을_찾은_음식만_이름과_사진으로_돌려준다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("fridge", List.of("파스타")));
        given(foodRecommender.recommend(List.of("파스타"))).willReturn(List.of("리조또", "뇨끼", "라자냐"));
        given(naverImageClient.findImageUrl("리조또")).willReturn(Optional.of("https://img/1.jpg"));
        given(naverImageClient.findImageUrl("뇨끼")).willReturn(Optional.empty());
        given(naverImageClient.findImageUrl("라자냐")).willReturn(Optional.of("https://img/3.jpg"));

        List<FoodRecommendationResponse> result = service.recommend(1L, 5L);

        assertThat(result).containsExactly(
                new FoodRecommendationResponse("리조또", "https://img/1.jpg"),
                new FoodRecommendationResponse("라자냐", "https://img/3.jpg"));
        verify(limiter).acquire(1L);
    }

    @Test
    void 일부_검색만_장애로_실패하면_나머지를_보여_준다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("fridge", List.of("파스타")));
        given(foodRecommender.recommend(List.of("파스타"))).willReturn(List.of("리조또", "뇨끼"));
        given(naverImageClient.findImageUrl("리조또")).willThrow(new NaverSearchException(new RuntimeException()));
        given(naverImageClient.findImageUrl("뇨끼")).willReturn(Optional.of("https://img/2.jpg"));

        assertThat(service.recommend(1L, 5L)).containsExactly(new FoodRecommendationResponse("뇨끼", "https://img/2.jpg"));
        verify(limiter, never()).release(1L, TODAY);
    }

    @Test
    void 모든_검색이_장애로_실패하면_횟수를_돌려주고_실패로_알린다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("fridge", List.of("파스타")));
        given(foodRecommender.recommend(List.of("파스타"))).willReturn(List.of("리조또", "뇨끼"));
        given(naverImageClient.findImageUrl(anyString())).willThrow(new NaverSearchException(new RuntimeException()));

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationFailedException.class);
        verify(limiter).release(1L, TODAY);
    }

    @Test
    void 모든_음식의_검색_결과가_없으면_장애가_아니므로_빈_목록이고_횟수는_유지한다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("fridge", List.of("파스타")));
        given(foodRecommender.recommend(List.of("파스타"))).willReturn(List.of("리조또"));
        given(naverImageClient.findImageUrl("리조또")).willReturn(Optional.empty());

        assertThat(service.recommend(1L, 5L)).isEmpty();
        verify(limiter, never()).release(1L, TODAY);
    }

    @Test
    void 냉장고가_아니면_횟수를_쓰지_않고_거절한다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("mirror", List.of("코트")));

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationUnsupportedException.class);
        verify(limiter, never()).acquire(1L);
    }

    @Test
    void 태그가_없으면_횟수를_쓰지_않고_거절한다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("fridge", List.of()));

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationTagRequiredException.class);
        verify(limiter, never()).acquire(1L);
    }

    @Test
    void AI가_실패하거나_비어_있으면_횟수를_돌려주고_실패로_알린다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("fridge", List.of("파스타")));
        given(foodRecommender.recommend(List.of("파스타"))).willReturn(List.of());

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationFailedException.class);
        verify(limiter).release(1L, TODAY);
    }
}

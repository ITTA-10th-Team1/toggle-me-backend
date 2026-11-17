package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemRecommendationServiceTest {

    private final ItemService itemService = mock(ItemService.class);
    private final FoodRecommender foodRecommender = mock(FoodRecommender.class);
    private final BookRecommender bookRecommender = mock(BookRecommender.class);
    private final NaverImageClient naverImageClient = mock(NaverImageClient.class);
    private final KakaoBookClient kakaoBookClient = mock(KakaoBookClient.class);
    private final ItemRecommendationLimiter limiter = mock(ItemRecommendationLimiter.class);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private final ItemRecommendationService service =
            new ItemRecommendationService(itemService, foodRecommender, bookRecommender, naverImageClient, kakaoBookClient, limiter);

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

        List<RecommendationResponse> result = service.recommend(1L, 5L);

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
    void 책장은_AI가_고른_제목마다_첫_검색_결과를_중복_없이_돌려준다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설", "잔잔한")));
        BookRecommendationResponse a = book("AA");
        BookRecommendationResponse b = book("BB");
        given(bookRecommender.recommend(List.of("소설", "잔잔한"))).willReturn(List.of("AA", "BB", "AA2", "없는책"));
        given(kakaoBookClient.search("AA", 5)).willReturn(List.of(a));
        given(kakaoBookClient.search("BB", 5)).willReturn(List.of(b));
        given(kakaoBookClient.search("AA2", 5)).willReturn(List.of(a));
        given(kakaoBookClient.search("없는책", 5)).willReturn(List.of());

        List<RecommendationResponse> result = service.recommend(1L, 5L);

        assertThat(result).containsExactly(a, b);
        verify(foodRecommender, never()).recommend(any());
        verify(limiter).acquire(1L);
    }

    @Test
    void 제목이_다른_책은_같은_책으로_보지_않고_띄어쓰기_기호_판본_차이는_같은_책으로_본다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        BookRecommendationResponse other = book("전혀 다른 책");
        BookRecommendationResponse edition = book("채식주의자 (스페셜 에디션)");
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("지어낸 책", "채식 주의자!"));
        given(kakaoBookClient.search("지어낸 책", 5)).willReturn(List.of(other));
        given(kakaoBookClient.search("채식 주의자!", 5)).willReturn(List.of(other, edition));

        assertThat(service.recommend(1L, 5L)).containsExactly(edition);
    }

    @Test
    void AI가_부제까지_붙인_제목도_카카오의_짧은_제목과_같은_책으로_본다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        BookRecommendationResponse shortTitle = book("채식주의자");
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("채식주의자 - 한강 장편소설"));
        given(kakaoBookClient.search("채식주의자 - 한강 장편소설", 5)).willReturn(List.of(shortTitle));

        assertThat(service.recommend(1L, 5L)).containsExactly(shortTitle);
    }

    @Test
    void 정규화하면_2자_미만인_제목은_검색하지_않고_뺀다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("봄", "!!!", "소설책"));
        given(kakaoBookClient.search("소설책", 5)).willReturn(List.of(book("봄"), book("소설책")));

        assertThat(service.recommend(1L, 5L)).containsExactly(book("소설책"));
        verify(kakaoBookClient, never()).search("봄", 5);
        verify(kakaoBookClient, never()).search("!!!", 5);
    }

    @Test
    void 짧아서_건너뛴_제목이_있어도_검색한_제목이_모두_장애면_실패로_알린다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("봄", "소설책"));
        given(kakaoBookClient.search("소설책", 5)).willThrow(new KakaoBookSearchException(new RuntimeException()));

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationFailedException.class);
        verify(limiter).release(1L, TODAY);
    }

    @Test
    void 제목이_모두_너무_짧으면_장애가_아니므로_빈_목록이고_횟수는_유지한다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("봄", "!!!"));

        assertThat(service.recommend(1L, 5L)).isEmpty();
        verify(limiter, never()).release(1L, TODAY);
    }

    @Test
    void 결과_제목이_2자_미만이면_어떤_제목과도_같은_책으로_보지_않는다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("봄날의 책"));
        given(kakaoBookClient.search("봄날의 책", 5)).willReturn(List.of(book("봄")));

        assertThat(service.recommend(1L, 5L)).isEmpty();
    }

    @Test
    void 책_검색은_동시에_보내도_AI가_고른_순서대로_돌려준다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("가가", "나나", "다다"));
        given(kakaoBookClient.search("가가", 5)).willAnswer(invocation -> {
            Thread.sleep(150); // 가장 느려도 순서는 바뀌지 않는다
            return List.of(book("가가"));
        });
        given(kakaoBookClient.search("나나", 5)).willReturn(List.of(book("나나")));
        given(kakaoBookClient.search("다다", 5)).willReturn(List.of(book("다다")));

        assertThat(service.recommend(1L, 5L)).containsExactly(book("가가"), book("나나"), book("다다"));
    }

    @Test
    void 책_검색이_일부만_장애면_나머지를_보여_준다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("AA", "BB"));
        given(kakaoBookClient.search("AA", 5)).willThrow(new KakaoBookSearchException(new RuntimeException()));
        given(kakaoBookClient.search("BB", 5)).willReturn(List.of(book("BB")));

        assertThat(service.recommend(1L, 5L)).containsExactly(book("BB"));
        verify(limiter, never()).release(any(), any());
    }

    @Test
    void 책_검색이_모두_장애면_횟수를_돌려주고_실패로_알린다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("AA"));
        given(kakaoBookClient.search("AA", 5)).willThrow(new KakaoBookSearchException(new RuntimeException()));

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationFailedException.class);
        verify(limiter).release(1L, TODAY);
    }

    @Test
    void AI가_책을_못_고르면_횟수를_돌려주고_실패로_알린다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of());

        assertThatThrownBy(() -> service.recommend(1L, 5L)).isInstanceOf(ItemRecommendationFailedException.class);
        verify(limiter).release(1L, TODAY);
    }

    @Test
    void 고른_책이_모두_검색에_없으면_장애가_아니므로_빈_목록이고_횟수는_유지한다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("없는책"));
        given(kakaoBookClient.search("없는책", 5)).willReturn(List.of());

        assertThat(service.recommend(1L, 5L)).isEmpty();
        verify(limiter, never()).release(1L, TODAY);
    }

    @Test
    void 책이_한_권이라도_있으면_횟수를_돌려주지_않는다() {
        given(itemService.getRecommendationSource(1L, 5L))
                .willReturn(new ItemRecommendationSource("bookshelf", List.of("소설")));
        given(bookRecommender.recommend(List.of("소설"))).willReturn(List.of("AA"));
        given(kakaoBookClient.search("AA", 5)).willReturn(List.of(book("AA")));

        assertThat(service.recommend(1L, 5L)).hasSize(1);
        verify(limiter, never()).release(any(), any());
    }

    private static BookRecommendationResponse book(String title) {
        return new BookRecommendationResponse(title, List.of("저자"), "https://img/" + title, "https://book/" + title);
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

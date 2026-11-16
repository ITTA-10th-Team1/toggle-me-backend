package com.itta.toggleme.item.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.itta.toggleme.auth.client.KakaoProperties;
import com.itta.toggleme.item.dto.BookRecommendationResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoBookClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final KakaoBookClient client = new KakaoBookClient(new KakaoProperties(1L, "rest-key"), builder);

    @Test
    void 표지가_https인_책만_돌려주고_키를_헤더로_보낸다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/v3/search/book?query=")))
                .andExpect(header("Authorization", "KakaoAK rest-key"))
                .andRespond(withSuccess("""
                        {"meta":{"total_count":3},"documents":[
                          {"title":"소설A","authors":["가","나"],"thumbnail":"https://t/a.jpg","url":"https://book/a","isbn":"1"},
                          {"title":"표지없음","authors":["다"],"thumbnail":"","url":"https://book/b"},
                          {"title":"소설C","authors":[],"thumbnail":"http://t/c.jpg","url":"https://book/c"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.search("소설", 3)).containsExactly(
                new BookRecommendationResponse("소설A", List.of("가", "나"), "https://t/a.jpg", "https://book/a"));
    }

    @Test
    void 저자_자리의_미사용인물코드와_빈_값은_뺀다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/")))
                .andRespond(withSuccess("""
                        {"documents":[
                          {"title":"교재","authors":["미사용인물코드"],"thumbnail":"https://t/a.jpg","url":"https://book/a"},
                          {"title":"공저","authors":["가","미사용인물코드"," "],"thumbnail":"https://t/b.jpg","url":"https://book/b"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.search("여행", 2)).extracting(BookRecommendationResponse::authors)
                .containsExactly(List.of(), List.of("가"));
    }

    @Test
    void 결과가_없으면_빈_목록이다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/")))
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.search("없는책", 2)).isEmpty();
    }

    @Test
    void 호출_자체가_실패하면_결과_없음과_구분해_예외를_던진다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://dapi.kakao.com/")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search("소설", 2)).isInstanceOf(KakaoBookSearchException.class);
    }
}

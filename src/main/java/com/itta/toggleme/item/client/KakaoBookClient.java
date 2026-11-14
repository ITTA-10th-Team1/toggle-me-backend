package com.itta.toggleme.item.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.itta.toggleme.auth.client.KakaoProperties;
import com.itta.toggleme.item.dto.BookRecommendationResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 카카오 다음 책 검색 API로 검색어(태그)에 맞는 책을 찾는다.
 * 검색 결과가 없으면 빈 목록이고, 호출 자체가 실패하면 {@link KakaoBookSearchException}을 던진다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoBookClient {

    private static final String BASE_URL = "https://dapi.kakao.com";

    private final RestClient restClient;

    @Autowired
    public KakaoBookClient(KakaoProperties properties) {
        this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
    }

    // 테스트에서 요청 팩토리가 가짜 서버로 바뀐 builder를 받는다
    KakaoBookClient(KakaoProperties properties, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "KakaoAK " + (properties.restApiKey() == null ? "" : properties.restApiKey()))
                .build();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return requestFactory;
    }

    // 표지(https)가 있는 책만 최대 size권. 검색 결과가 없으면 빈 목록이다
    public List<BookRecommendationResponse> search(String query, int size) {
        try {
            SearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/v3/search/book")
                            .queryParam("query", "{query}")
                            .queryParam("size", size)
                            .build(query))
                    .retrieve()
                    .body(SearchResponse.class);
            if (response == null || response.documents() == null) {
                return List.of();
            }
            return response.documents().stream()
                    .filter(d -> d.title() != null && !d.title().isBlank())
                    .filter(d -> d.thumbnail() != null && d.thumbnail().startsWith("https://"))
                    .map(d -> new BookRecommendationResponse(d.title(), realAuthors(d.authors()), d.thumbnail(), d.url()))
                    .toList();
        } catch (RuntimeException e) {
            // 검색어는 로그에 남기지 않는다
            log.warn("Kakao book search failed: {}", e.getClass().getSimpleName());
            throw new KakaoBookSearchException(e);
        }
    }

    // 카카오 데이터에 저자 자리에 들어 있는 자리표시 값(예: 미사용인물코드)은 화면에 보이지 않게 뺀다
    private static List<String> realAuthors(List<String> authors) {
        if (authors == null) {
            return List.of();
        }
        return authors.stream().filter(a -> a != null && !a.isBlank() && !a.contains("미사용인물코드")).toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(List<Document> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Document(String title, List<String> authors, String thumbnail, String url) {
    }
}

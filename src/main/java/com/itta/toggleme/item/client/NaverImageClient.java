package com.itta.toggleme.item.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * NAVER API HUB 검색 API(이미지)로 음식 이름의 대표 사진 주소를 찾는다.
 * 검색 결과가 없으면 빈 값이고, 호출 자체가 실패하면 {@link NaverSearchException}을 던진다.
 * 호출한 쪽이 "결과 없음"과 "장애"를 구분해 횟수 차감과 안내를 정한다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(NaverProperties.class)
public class NaverImageClient {
    // 2026년에 검색 API가 개발자센터(openapi.naver.com)에서 NAVER API HUB(NCP 콘솔 발급 키)로 옮겨졌다.
    // 개발자센터 키는 여기서 쓸 수 없다

    private static final String BASE_URL = "https://naverapihub.apigw.ntruss.com";

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RestClient restClient;

    @Autowired
    public NaverImageClient(NaverProperties properties) {
        this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
    }

    // 테스트에서 요청 팩토리가 가짜 서버로 바뀐 builder를 받는다
    NaverImageClient(NaverProperties properties, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", properties.clientId() == null ? "" : properties.clientId())
                .defaultHeader("X-NCP-APIGW-API-KEY", properties.clientSecret() == null ? "" : properties.clientSecret())
                .build();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return requestFactory;
    }

    // 결과 중 첫 번째 썸네일 주소. 검색 결과가 없으면 비어 있다
    public Optional<String> findImageUrl(String query) {
        try {
            // 응답이 JSON인데 Content-Type이 text/plain이라 자동 변환이 거부한다. 문자열로 받아 직접 읽는다
            String body = restClient.get()
                    .uri(uri -> uri.path("/search/v1/image")
                            .queryParam("query", "{query}")
                            .queryParam("display", 5)
                            .queryParam("filter", "large")
                            .build(query))
                    .retrieve()
                    .body(String.class);
            SearchResponse response = body == null ? null : JSON_MAPPER.readValue(body, SearchResponse.class);
            if (response == null || response.items() == null) {
                return Optional.empty();
            }
            return response.items().stream()
                    .map(Item::thumbnail)
                    .filter(url -> url != null && url.startsWith("https://"))
                    .findFirst();
        } catch (RuntimeException e) {
            // 검색어는 로그에 남기지 않는다
            log.warn("Naver image search failed: {}", e.getClass().getSimpleName());
            throw new NaverSearchException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String thumbnail) {
    }
}

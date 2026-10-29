package com.itta.toggleme.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itta.toggleme.auth.exception.InvalidKakaoTokenException;
import com.itta.toggleme.auth.exception.KakaoApiException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
@EnableConfigurationProperties(KakaoProperties.class)
public class KakaoClient {

    private static final String KAKAO_API_BASE_URL = "https://kapi.kakao.com";

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    public KakaoClient(KakaoProperties kakaoProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(KAKAO_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
        this.kakaoProperties = kakaoProperties;
    }

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        verifyIssuedForThisApp(kakaoAccessToken);
        KakaoUserResponse response = get("/v2/user/me", kakaoAccessToken, KakaoUserResponse.class);
        return new KakaoUserInfo(String.valueOf(response.id()), response.nickname());
    }

    private void verifyIssuedForThisApp(String kakaoAccessToken) {
        KakaoTokenInfoResponse tokenInfo = get("/v1/user/access_token_info", kakaoAccessToken,
                KakaoTokenInfoResponse.class);
        if (!Objects.equals(tokenInfo.appId(), kakaoProperties.appId())) {
            throw new InvalidKakaoTokenException();
        }
    }

    private <T> T get(String uri, String kakaoAccessToken, Class<T> responseType) {
        try {
            T body = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.UNAUTHORIZED), (request, response) -> {
                        throw new InvalidKakaoTokenException();
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new KakaoApiException();
                    })
                    .body(responseType);
            if (body == null) {
                throw new KakaoApiException();
            }
            return body;
        } catch (ResourceAccessException e) {
            throw new KakaoApiException();
        }
    }

    private record KakaoTokenInfoResponse(
            Long id,
            @JsonProperty("app_id") Long appId
    ) {
    }

    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {

        String nickname() {
            if (kakaoAccount == null || kakaoAccount.profile() == null) {
                return null;
            }
            return kakaoAccount.profile().nickname();
        }
    }

    private record KakaoAccount(
            Profile profile
    ) {
    }

    private record Profile(
            String nickname
    ) {
    }
}

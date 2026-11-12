package com.itta.toggleme.auth.client;

import com.itta.toggleme.auth.exception.TossApiException;
import com.itta.toggleme.auth.exception.TossAuthorizationCodeInvalidException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 앱인토스 토스 로그인 서버 API 호출. 인가 코드로 토큰을 받고 사용자 키(userKey)를 조회한다.
 * 토스 토큰과 개인정보는 저장하지 않는다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(TossProperties.class)
public class TossClient {

    private static final String GENERATE_TOKEN_URI = "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";
    private static final String LOGIN_ME_URI = "/api-partner/v1/apps-in-toss/user/oauth2/login-me";
    private static final ParameterizedTypeReference<Map<String, Object>> BODY_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    @Autowired
    public TossClient(TossProperties properties) {
        this(buildRestClient(properties));
    }

    TossClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public TossUserInfo getUserInfo(String authorizationCode, String referrer) {
        String accessToken = issueAccessToken(authorizationCode, referrer);
        String userKey = fetchUserKey(accessToken);
        return new TossUserInfo(userKey);
    }

    private String issueAccessToken(String authorizationCode, String referrer) {
        Map<String, Object> body = call(true, () -> restClient.post()
                .uri(GENERATE_TOKEN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("authorizationCode", authorizationCode, "referrer", referrer)));
        return requireText(body.get("accessToken"));
    }

    private String fetchUserKey(String accessToken) {
        Map<String, Object> body = call(false, () -> restClient.get()
                .uri(LOGIN_ME_URI)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
        return requireText(body.get("userKey"));
    }

    /**
     * 응답이 {"resultType":"SUCCESS","success":{...}} 로 감싸져 오든 바로 오든 같은 결과를 돌려준다.
     * 문서에서 두 형태가 모두 보여서 어느 쪽이 와도 동작하게 둔다.
     */
    private Map<String, Object> call(boolean invalidGrantMeansInvalidCode,
                                     Supplier<RestClient.RequestHeadersSpec<?>> request) {
        try {
            Result result = request.get().exchange((req, res) -> {
                Map<String, Object> body = res.bodyTo(BODY_TYPE);
                return new Result(res.getStatusCode(), body == null ? Map.of() : body);
            });
            return unwrap(result, invalidGrantMeansInvalidCode);
        } catch (RestClientException e) {
            // 인가 코드·토큰이 담길 수 있는 메시지는 남기지 않고 예외 종류만 남긴다
            log.warn("토스 API 호출 실패 cause={}", e.getClass().getSimpleName());
            throw new TossApiException();
        }
    }

    private Map<String, Object> unwrap(Result result, boolean invalidGrantMeansInvalidCode) {
        Map<String, Object> body = result.body();
        Object error = body.get("error");
        if (invalidGrantMeansInvalidCode && "invalid_grant".equals(error)) {
            throw new TossAuthorizationCodeInvalidException();
        }
        // 실제 응답은 실패해도 HTTP 200이고 {"resultType":"FAIL","success":null,"error":{...}} 모양이다.
        // 성공 응답에도 "error":null 키가 올 수 있어서 키 존재가 아니라 값으로 판단한다
        if (!result.status().is2xxSuccessful() || "FAIL".equals(body.get("resultType")) || error != null) {
            logFailure(result.status(), error);
            throw new TossApiException();
        }
        if (body.get("success") instanceof Map<?, ?> success) {
            Map<String, Object> unwrapped = new HashMap<>();
            success.forEach((key, value) -> unwrapped.put(String.valueOf(key), value));
            return unwrapped;
        }
        return body;
    }

    /** 원인 파악용 로그. 인가 코드, 토큰, 요청·응답 본문 전체는 남기지 않는다. */
    private void logFailure(HttpStatusCode status, Object error) {
        if (error instanceof Map<?, ?> detail) {
            log.warn("토스 API 실패 status={} errorCode={} reason={}",
                    status.value(), detail.get("errorCode"), detail.get("reason"));
            return;
        }
        log.warn("토스 API 실패 status={} error={}", status.value(), error);
    }

    private String requireText(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            log.warn("토스 API 응답에 필요한 값이 없습니다.");
            throw new TossApiException();
        }
        return String.valueOf(value);
    }

    private static RestClient buildRestClient(TossProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext(properties))
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static SSLContext sslContext(TossProperties properties) {
        if (properties.hasMtlsFiles()) {
            try {
                return MtlsSslContextFactory.create(Path.of(properties.certPath()), Path.of(properties.keyPath()));
            } catch (IllegalStateException e) {
                // 경로 오타나 마운트 누락으로 서버 전체가 내려가지 않게 한다. 토스 로그인만 502로 실패한다
                log.error("토스 mTLS 인증서를 읽지 못해 토스 로그인이 동작하지 않습니다. cause={}", e.getMessage());
            }
        } else {
            log.warn("토스 mTLS 인증서 경로가 설정되지 않아 토스 로그인이 동작하지 않습니다.");
        }
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Result(HttpStatusCode status, Map<String, Object> body) {
    }
}

package com.itta.toggleme.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

// appId는 로그인 토큰 검증용, restApiKey는 책 검색(다음 책 검색 API) 호출용이다. restApiKey가 비면 책 검색이 모두 실패로 처리된다
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        Long appId,
        String restApiKey
) {
}

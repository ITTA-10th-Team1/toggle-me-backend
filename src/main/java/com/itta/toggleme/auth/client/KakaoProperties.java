package com.itta.toggleme.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 카카오 로그인(auth)과 책 검색(item)이 함께 쓰는 설정이다. 로그인 설정을 손볼 때 지우지 않도록 용도를 적어 둔다.
// appId: 로그인 토큰 검증용 (KakaoClient)
// restApiKey: 책장 추천의 책 검색(다음 책 검색 API) 호출용 (KakaoBookClient). 비면 책 검색이 모두 실패로 처리된다
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        Long appId,
        String restApiKey
) {
}

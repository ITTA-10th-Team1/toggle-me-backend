package com.itta.toggleme.item.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

// NAVER API HUB(NCP 콘솔 > NAVER API HUB > Application)에서 발급한 Client ID·Secret. 비어 있으면 이미지 검색이 모두 실패로 처리된다
@ConfigurationProperties(prefix = "naver")
public record NaverProperties(
        String clientId,
        String clientSecret
) {
}

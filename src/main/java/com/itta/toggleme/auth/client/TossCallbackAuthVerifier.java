package com.itta.toggleme.auth.client;

import com.itta.toggleme.auth.exception.TossCallbackUnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 토스 콜백의 Basic Auth 확인. 아이디·비밀번호가 설정되지 않았으면 모든 콜백을 거절한다.
 */
@Component
public class TossCallbackAuthVerifier {

    private static final String BASIC_PREFIX = "Basic ";

    private final String expected;

    public TossCallbackAuthVerifier(TossProperties properties) {
        boolean configured = notBlank(properties.callbackUsername()) && notBlank(properties.callbackPassword());
        this.expected = configured ? properties.callbackUsername() + ":" + properties.callbackPassword() : null;
    }

    public void verify(String authorizationHeader) {
        if (expected == null || authorizationHeader == null || !authorizationHeader.startsWith(BASIC_PREFIX)) {
            throw new TossCallbackUnauthorizedException();
        }
        byte[] actual;
        try {
            actual = Base64.getDecoder().decode(authorizationHeader.substring(BASIC_PREFIX.length()).trim());
        } catch (IllegalArgumentException e) {
            throw new TossCallbackUnauthorizedException();
        }
        // 시간 차이로 값을 추측하지 못하게 상수 시간으로 비교한다
        if (!MessageDigest.isEqual(actual, expected.getBytes(StandardCharsets.UTF_8))) {
            throw new TossCallbackUnauthorizedException();
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

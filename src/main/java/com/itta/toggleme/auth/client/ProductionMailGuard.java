package com.itta.toggleme.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 운영(prod)에서 메일 설정이 비어 있으면 서버가 뜨지 않게 한다.
 * 설정을 빠뜨린 채 뜨면 사용자는 인증 코드를 받지 못하고, 코드가 서버 로그로 가는 일을 막기 위해서다.
 */
@Component
@Profile("prod")
public class ProductionMailGuard {

    public ProductionMailGuard(@Value("${spring.mail.username:}") String username,
                               @Value("${spring.mail.password:}") String password) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "운영에서는 MAIL_USERNAME과 MAIL_PASSWORD가 필요합니다. 서버 .env에 설정해 주세요.");
        }
    }
}

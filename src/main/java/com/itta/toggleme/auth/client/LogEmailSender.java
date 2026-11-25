package com.itta.toggleme.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * MAIL_USERNAME이 없을 때 쓰는 개발용 발송기. 메일을 보내지 않고 코드를 로그로 남긴다.
 * 운영에서 메일 설정을 빠뜨리면 코드가 서버 로그에 남으므로 기동 때 경고한다.
 */
@Slf4j
@Component
@ConditionalOnExpression("'${spring.mail.username:}'.isBlank()")
public class LogEmailSender implements EmailSender {

    public LogEmailSender() {
        log.warn("메일 설정(MAIL_USERNAME)이 없어 인증 메일을 보내지 않고 코드를 로그로 남깁니다. 로컬 개발용입니다.");
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("[개발용] 이메일 인증 코드 to={} code={}", to, code);
    }
}

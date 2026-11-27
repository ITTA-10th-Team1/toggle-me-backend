package com.itta.toggleme.auth.client;

import com.itta.toggleme.auth.exception.EmailSendFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP(Gmail)로 인증 메일을 보낸다. MAIL_USERNAME이 설정돼 있을 때만 쓴다. 코드와 수신 주소는 로그에 남기지 않는다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${spring.mail.username:}'.isBlank()")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerificationCode(String to, String code) {
        send(buildMessage(to, "[ToggleMe] 이메일 인증 코드", "ToggleMe 이메일 인증 코드는 " + code + " 입니다.\n"
                + "10분 안에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시해 주세요."));
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        send(buildMessage(to, "[ToggleMe] 비밀번호 재설정 코드", "ToggleMe 비밀번호 재설정 코드는 " + code + " 입니다.\n"
                + "10분 안에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시해 주세요."));
    }

    private void send(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("인증 메일 발송 실패 cause={}", e.getClass().getSimpleName());
            throw new EmailSendFailedException();
        }
    }

    private SimpleMailMessage buildMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }
}

package com.itta.toggleme.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.itta.toggleme.auth.exception.EmailSendFailedException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSender sender = senderWith(mailSender);

    @Test
    void 인증_코드를_담아_발신자와_수신자로_메일을_보낸다() {
        sender.sendVerificationCode("user@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getText()).contains("123456");
    }

    @Test
    void 비밀번호_재설정_코드는_다른_제목으로_보낸다() {
        sender.sendPasswordResetCode("user@example.com", "654321");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("비밀번호 재설정");
        assertThat(captor.getValue().getText()).contains("654321");
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
    }

    @Test
    void 발송에_실패하면_발송_실패_예외로_바꾸고_코드와_주소를_로그에_남기지_않는다() {
        Logger logger = (Logger) LoggerFactory.getLogger(SmtpEmailSender.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            willThrow(new MailSendException("smtp down: user@example.com 123456"))
                    .given(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> sender.sendVerificationCode("user@example.com", "123456"))
                    .isInstanceOf(EmailSendFailedException.class);

            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .isNotEmpty()
                    .noneMatch(message -> message.contains("123456") || message.contains("user@example.com"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static SmtpEmailSender senderWith(JavaMailSender mailSender) {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender);
        ReflectionTestUtils.setField(sender, "from", "noreply@example.com");
        return sender;
    }
}

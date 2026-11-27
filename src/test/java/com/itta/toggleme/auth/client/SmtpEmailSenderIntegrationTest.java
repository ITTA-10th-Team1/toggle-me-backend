package com.itta.toggleme.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 메일 설정(MAIL_USERNAME)이 있을 때 실제 Spring 설정으로 SmtpEmailSender가 선택되고, SMTP로 인증 코드 메일이
 * 나가는지 로컬 가짜 SMTP 서버로 확인한다. 외부 Gmail에는 연결하지 않는다(TLS·로그인은 이 테스트에서 끈다).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SmtpEmailSenderIntegrationTest {

    private static final FakeSmtpServer SMTP = FakeSmtpServer.start();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String email;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> SMTP.port());
        registry.add("spring.mail.username", () -> "noreply@example.com");
        registry.add("spring.mail.password", () -> "app-password");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @AfterEach
    void cleanUp() {
        if (email != null) {
            jdbcTemplate.update("delete from email_verification where email = ?", email);
        }
    }

    @AfterAll
    static void stopServer() {
        SMTP.close();
    }

    @Test
    void 메일_설정이_있으면_SMTP_발송기를_쓴다() {
        assertThat(emailSender).isInstanceOf(SmtpEmailSender.class);
    }

    @Test
    void 메일_서버_연결은_상태_확인에_포함되지_않는다() {
        // 포함되면 메일 서버 장애나 로그인 거부가 /actuator/health 503이 되어 배포 상태 확인이 실패한다
        assertThat(applicationContext.getBeanDefinitionNames())
                .noneMatch(name -> name.toLowerCase().contains("mailhealth"));
    }

    @Test
    void 코드를_요청하면_SMTP로_인증_코드_메일이_발송된다() throws Exception {
        email = "smtp-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";

        mockMvc.perform(post("/api/v1/auth/email-verifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isNoContent());

        MimeMessage message = SMTP.awaitMessage();
        assertThat(message.getFrom()[0].toString()).isEqualTo("noreply@example.com");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo(email);
        assertThat(message.getSubject()).isEqualTo("[ToggleMe] 이메일 인증 코드");
        assertThat((String) message.getContent()).containsPattern("\\d{6}");
    }

    /** MAIL FROM, RCPT TO, DATA만 받아 주는 최소한의 SMTP 서버. 받은 메일을 큐에 쌓는다. */
    private static final class FakeSmtpServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final BlockingQueue<String> received = new LinkedBlockingQueue<>();

        private FakeSmtpServer(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        static FakeSmtpServer start() {
            try {
                FakeSmtpServer server = new FakeSmtpServer(new ServerSocket(0, 5, InetAddress.getLoopbackAddress()));
                Thread thread = new Thread(server::acceptLoop, "fake-smtp");
                thread.setDaemon(true);
                thread.start();
                return server;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        MimeMessage awaitMessage() throws Exception {
            String raw = received.poll(10, TimeUnit.SECONDS);
            assertThat(raw).as("SMTP 서버가 받은 메일").isNotNull();
            return new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(raw.getBytes(StandardCharsets.ISO_8859_1)));
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    handle(socket);
                } catch (IOException e) {
                    // 서버 종료 또는 연결 끊김
                }
            }
        }

        private void handle(Socket socket) throws IOException {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.ISO_8859_1);
            out.print("220 fake-smtp ready\r\n");
            out.flush();
            String line;
            while ((line = in.readLine()) != null) {
                String command = line.toUpperCase();
                if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                    reply(out, "250 fake-smtp");
                } else if (command.startsWith("DATA")) {
                    reply(out, "354 end with .");
                    StringBuilder data = new StringBuilder();
                    while ((line = in.readLine()) != null && !line.equals(".")) {
                        data.append(line).append("\r\n");
                    }
                    received.add(data.toString());
                    reply(out, "250 ok");
                } else if (command.startsWith("QUIT")) {
                    reply(out, "221 bye");
                    return;
                } else {
                    reply(out, "250 ok");
                }
            }
        }

        private void reply(PrintWriter out, String text) {
            out.print(text + "\r\n");
            out.flush();
        }

        @Override
        public void close() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // 종료 중 오류는 무시한다
            }
        }
    }
}

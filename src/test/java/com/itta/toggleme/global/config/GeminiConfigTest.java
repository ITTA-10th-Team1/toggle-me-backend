package com.itta.toggleme.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.genai.ApiClient;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class GeminiConfigTest {

    private static Environment environmentWithKey(String key) {
        return new MockEnvironment().withProperty(GeminiConfig.API_KEY_PROPERTY, key);
    }

    @Test
    void 설정의_키를_그대로_쓰고_자리표시_글자를_넘기지_않는다() {
        Client client = new GeminiConfig().googleGenAiClient(environmentWithKey("real-key"));

        assertThat(client.apiKey()).isEqualTo("real-key").doesNotContain("${");
    }

    @Test
    void 호출_timeout과_재시도_횟수를_건다() {
        Client client = new GeminiConfig().googleGenAiClient(environmentWithKey("test-key"));

        HttpOptions options = ((ApiClient) ReflectionTestUtils.getField(client, "apiClient")).httpOptions();
        assertThat(options.timeout()).contains(GeminiConfig.TIMEOUT_MILLIS);
        assertThat(options.retryOptions().flatMap(HttpRetryOptions::attempts)).contains(GeminiConfig.ATTEMPTS);
        assertThat(client.apiKey()).isEqualTo("test-key");
    }

    @Test
    void 응답이_없으면_timeout으로_끊는다() throws IOException {
        // 요청을 받고 10초 동안 답하지 않는 서버. timeout 1초 × 2번이면 10초보다 훨씬 먼저 끝나야 한다
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException ignored) {
            }
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write("{}".getBytes());
            }
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        try {
            Client client = Client.builder()
                    .apiKey("test-key")
                    .httpOptions(HttpOptions.builder()
                            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .timeout(1_000)
                            .retryOptions(HttpRetryOptions.builder().attempts(GeminiConfig.ATTEMPTS))
                            .build())
                    .build();

            long start = System.nanoTime();
            assertThatThrownBy(() -> client.models.generateContent("gemini-3.1-flash-lite", "hi", null))
                    .isInstanceOf(RuntimeException.class);
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);
        } finally {
            server.stop(0);
        }
    }
}

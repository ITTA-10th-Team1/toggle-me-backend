package com.itta.toggleme.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 실제 스프링 컨텍스트에서 Client가 설정의 키를 받는지 확인한다(@Value 미해석으로 "${...}"가 전달되던 문제의 회귀 방지)
@SpringBootTest(properties = "spring.ai.google.genai.api-key=context-test-key")
class GeminiConfigIntegrationTest {

    @Autowired
    private Client client;

    @Test
    void 컨텍스트의_Client는_설정의_키를_쓴다() {
        assertThat(client.apiKey()).isEqualTo("context-test-key");
    }
}

package com.itta.toggleme.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring AI 기본 Client는 호출 timeout이 없고 SDK가 실패 시 5번까지 재시도해 한 건이 수 분씩 걸렸다.
// 이 Bean이 있으면 Spring AI는 자기 Client를 만들지 않는다(@ConditionalOnMissingBean)
@Configuration
public class GeminiConfig {

    // timeout은 시도마다 걸린다. 20초 × 2번 + 재시도 대기 약 1초로 한 건이 최대 약 42초.
    // 앱은 분석 결과를 60초 기다리므로 나머지는 대기열 대기 몫이다
    static final int TIMEOUT_MILLIS = 20_000;
    // 첫 호출 포함 횟수. 응답이 없거나 429·5xx면 한 번 더 시도한다
    static final int ATTEMPTS = 2;

    @Bean
    public Client googleGenAiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(TIMEOUT_MILLIS)
                        .retryOptions(HttpRetryOptions.builder().attempts(ATTEMPTS))
                        .build())
                .build();
    }
}

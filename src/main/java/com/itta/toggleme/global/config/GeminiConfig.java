package com.itta.toggleme.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

// Spring AI 기본 Client는 호출 timeout이 없고 SDK가 실패 시 5번까지 재시도해 한 건이 수 분씩 걸렸다.
// 이 Bean이 있으면 Spring AI는 자기 Client를 만들지 않는다(@ConditionalOnMissingBean)
@Configuration
public class GeminiConfig {

    // timeout은 시도마다 걸린다. 20초 × 2번 + 재시도 대기 약 1초로 한 건이 최대 약 42초.
    // 앱은 분석 결과를 60초 기다리므로 나머지는 대기열 대기 몫이다
    static final int TIMEOUT_MILLIS = 20_000;
    // 첫 호출 포함 횟수. 응답이 없거나 429·5xx면 한 번 더 시도한다
    static final int ATTEMPTS = 2;

    static final String API_KEY_PROPERTY = "spring.ai.google.genai.api-key";

    // @Value로 받으면 이 설정이 속성 처리기보다 먼저 만들어질 때 키가 풀리지 않고 "${spring.ai.google.genai.api-key}"
    // 글자 그대로 Gemini에 전달돼 API_KEY_INVALID가 났다. Environment에서 직접 읽으면 항상 실제 값이다
    @Bean
    public Client googleGenAiClient(Environment environment) {
        return Client.builder()
                .apiKey(environment.getProperty(API_KEY_PROPERTY, ""))
                .httpOptions(HttpOptions.builder()
                        .timeout(TIMEOUT_MILLIS)
                        .retryOptions(HttpRetryOptions.builder().attempts(ATTEMPTS))
                        .build())
                .build();
    }
}

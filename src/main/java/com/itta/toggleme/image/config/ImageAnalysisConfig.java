package com.itta.toggleme.image.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// 대기열에는 분석 ID만 들어간다. 사진은 작업 스레드가 차례를 받은 뒤에야 다룬다
@Configuration
public class ImageAnalysisConfig {

    private static final int QUEUE_CAPACITY = 100;
    // ponytail: Gemini 호출 자체의 timeout은 없다. Client를 직접 만들어 timeout을 주면 API 키 인증이 실패해서 뺐다.
    // 앱에는 ImageAnalysis의 60초 만료로 결과를 보장한다. 호출이 오래 묶여 대기열이 밀리면 timeout 방법을 다시 찾는다

    // JPEG·PNG는 Gemini가 presigned URL로 직접 읽는다. 서버 heap을 거의 쓰지 않아 여러 건을 함께 처리한다
    @Bean
    public ThreadPoolTaskExecutor urlImageAnalysisExecutor() {
        return executor(4, "image-analysis-url-");
    }

    // HEIC는 Gemini URL 입력이 지원되지 않아 서버가 바이트로 보낸다.
    // 10MB 사진 2건을 동시에 보내면 heap 256MB에서 OOM이 나서 1건씩 처리한다
    @Bean
    public ThreadPoolTaskExecutor inlineImageAnalysisExecutor() {
        return executor(1, "image-analysis-inline-");
    }

    private ThreadPoolTaskExecutor executor(int workers, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(threadNamePrefix);
        return executor;
    }
}

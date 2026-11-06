package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.item.exception.ItemRecommendationLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ItemRecommendationLimiterTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-06T01:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return SEOUL;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };
    private final ItemRecommendationLimiter limiter = new ItemRecommendationLimiter(clock);

    @Test
    void 하루_10회까지_쓰고_11번째는_막는다() {
        for (int i = 0; i < ItemRecommendationLimiter.DAILY_LIMIT; i++) {
            limiter.acquire(1L);
        }

        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(ItemRecommendationLimitExceededException.class);
    }

    @Test
    void 횟수는_회원별로_센다() {
        for (int i = 0; i < ItemRecommendationLimiter.DAILY_LIMIT; i++) {
            limiter.acquire(1L);
        }

        assertThatCode(() -> limiter.acquire(2L)).doesNotThrowAnyException();
    }

    @Test
    void 서울_기준_날짜가_바뀌면_다시_쓸_수_있다() {
        for (int i = 0; i < ItemRecommendationLimiter.DAILY_LIMIT; i++) {
            limiter.acquire(1L);
        }
        // 11-06 10:00(서울)에 다 썼고, 15:00Z는 서울 기준 11-07 00:00이다
        now.set(Instant.parse("2026-11-06T15:00:00Z"));

        assertThatCode(() -> limiter.acquire(1L)).doesNotThrowAnyException();
    }

    @Test
    void 돌려준_횟수는_다시_쓸_수_있다() {
        for (int i = 0; i < ItemRecommendationLimiter.DAILY_LIMIT; i++) {
            limiter.acquire(1L);
        }
        limiter.release(1L);

        assertThatCode(() -> limiter.acquire(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(ItemRecommendationLimitExceededException.class);
    }
}

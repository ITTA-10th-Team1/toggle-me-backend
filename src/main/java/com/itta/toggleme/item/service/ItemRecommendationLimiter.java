package com.itta.toggleme.item.service;

import com.itta.toggleme.item.exception.ItemRecommendationLimitExceededException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 회원당 하루 AI 추천 횟수 제한. 서버 메모리로 세므로 서버가 재시작되면 횟수가 초기화된다(서버 1대인 MVP 기준).
 * 서버를 여러 대로 늘리면 DB나 Redis로 옮긴다. 날짜는 서비스 기준(Asia/Seoul)이다.
 */
@Component
public class ItemRecommendationLimiter {

    static final int DAILY_LIMIT = 10;
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private record Usage(LocalDate date, int count) {
    }

    private final ConcurrentHashMap<Long, Usage> usages = new ConcurrentHashMap<>();
    private final Clock clock;

    public ItemRecommendationLimiter() {
        this(Clock.system(SERVICE_ZONE));
    }

    ItemRecommendationLimiter(Clock clock) {
        this.clock = clock;
    }

    // 횟수를 하나 쓴다. 한도를 넘으면 예외
    public void acquire(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        boolean[] exceeded = {false};
        usages.compute(memberId, (id, usage) -> {
            int used = usage == null || !usage.date().equals(today) ? 0 : usage.count();
            if (used >= DAILY_LIMIT) {
                exceeded[0] = true;
                return usage;
            }
            return new Usage(today, used + 1);
        });
        if (exceeded[0]) {
            throw new ItemRecommendationLimitExceededException();
        }
    }

    // AI나 검색이 실패했을 때 쓴 횟수를 돌려준다
    public void release(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        usages.computeIfPresent(memberId, (id, usage) ->
                usage.date().equals(today) && usage.count() > 0 ? new Usage(today, usage.count() - 1) : usage);
    }
}

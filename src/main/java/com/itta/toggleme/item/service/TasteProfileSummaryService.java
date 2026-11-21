package com.itta.toggleme.item.service;

import com.itta.toggleme.item.dto.TasteProfileResponse;
import com.itta.toggleme.item.dto.TasteProfileSummaryResponse;
import com.itta.toggleme.item.dto.TasteTagResponse;
import com.itta.toggleme.item.exception.TasteProfileNotEnoughException;
import com.itta.toggleme.item.exception.TasteProfileSummaryFailedException;
import com.itta.toggleme.item.repository.TasteTagCount;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 취향 카드의 "유형명" 버튼. 취향 카드와 같은 규칙으로 모은 태그만 AI에게 보내 유형명과 한 줄 설명을 받는다.
 * 결과는 저장하지 않는다(누를 때마다 새로 만든다). 외부 호출이 길어서 DB 트랜잭션을 잡지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TasteProfileSummaryService {

    private static final int MAX_TYPE_NAME_LENGTH = 20;
    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private final TasteProfileService tasteProfileService;
    private final TasteProfileSummarizer summarizer;
    private final TasteProfileLimiter limiter;

    public TasteProfileSummaryResponse summarize(Long memberId) {
        TasteProfileResponse profile = tasteProfileService.getProfile(memberId);
        // 기록이 모자란 요청이나 태그가 하나도 없는 요청은 AI를 부르지 않고 횟수도 쓰지 않는다
        if (!profile.eligible() || profile.tags().isEmpty()) {
            throw new TasteProfileNotEnoughException();
        }
        List<TasteTagCount> tags = profile.tags().stream()
                .map(tag -> new TasteTagCount(tag.name(), tag.count()))
                .toList();

        LocalDate acquiredOn = limiter.acquire(memberId);
        TasteProfileSummary summary;
        try {
            summary = summarizer.summarize(tags);
            if (summary == null || !isValid(summary)) {
                throw new IllegalStateException("invalid summary");
            }
        } catch (RuntimeException e) {
            // 장애나 쓸 수 없는 답으로 실패하면 횟수를 돌려준다(요청을 시작한 날짜 기준)
            limiter.release(memberId, acquiredOn);
            log.warn("Taste profile summary failed: {}", e.getClass().getSimpleName());
            throw new TasteProfileSummaryFailedException();
        }
        return new TasteProfileSummaryResponse(
                summary.typeName(),
                summary.description(),
                profile.tags().stream().map(TasteTagResponse::name).toList());
    }

    private static boolean isValid(TasteProfileSummary summary) {
        return !summary.typeName().isEmpty() && summary.typeName().length() <= MAX_TYPE_NAME_LENGTH
                && !summary.description().isEmpty() && summary.description().length() <= MAX_DESCRIPTION_LENGTH;
    }
}

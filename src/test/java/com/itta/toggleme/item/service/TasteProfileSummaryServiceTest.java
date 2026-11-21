package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.itta.toggleme.item.dto.TasteProfileResponse;
import com.itta.toggleme.item.dto.TasteProfileSummaryResponse;
import com.itta.toggleme.item.dto.TasteTagResponse;
import com.itta.toggleme.item.exception.TasteProfileLimitExceededException;
import com.itta.toggleme.item.exception.TasteProfileNotEnoughException;
import com.itta.toggleme.item.exception.TasteProfileSummaryFailedException;
import com.itta.toggleme.item.repository.TasteTagCount;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TasteProfileSummaryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    private final TasteProfileService profileService = mock(TasteProfileService.class);
    private final TasteProfileSummarizer summarizer = mock(TasteProfileSummarizer.class);
    private final TasteProfileLimiter limiter = mock(TasteProfileLimiter.class);
    private final TasteProfileSummaryService service =
            new TasteProfileSummaryService(profileService, summarizer, limiter);

    private static TasteProfileResponse eligible() {
        return new TasteProfileResponse(30, TODAY.minusDays(29), TODAY, 6, 5, true,
                List.of(new TasteTagResponse("추리", 4), new TasteTagResponse("외국소설", 3)));
    }

    @Test
    void 태그와_횟수만_AI에_보내고_유형명_설명_태그를_돌려준다() {
        given(profileService.getProfile(1L)).willReturn(eligible());
        given(limiter.acquire(1L)).willReturn(TODAY);
        given(summarizer.summarize(any())).willReturn(new TasteProfileSummary(" 주린징어 ", "추리를 좋아하는 사람"));

        TasteProfileSummaryResponse result = service.summarize(1L);

        assertThat(result).isEqualTo(new TasteProfileSummaryResponse(
                "주린징어", "추리를 좋아하는 사람", List.of("추리", "외국소설")));
        verify(summarizer).summarize(List.of(new TasteTagCount("추리", 4), new TasteTagCount("외국소설", 3)));
        verify(limiter, never()).release(any(), any());
    }

    @Test
    void 기록이_5개_미만이면_AI도_횟수도_쓰지_않고_거절한다() {
        given(profileService.getProfile(1L)).willReturn(
                new TasteProfileResponse(30, TODAY.minusDays(29), TODAY, 4, 5, false, List.of()));

        assertThatThrownBy(() -> service.summarize(1L)).isInstanceOf(TasteProfileNotEnoughException.class);
        verifyNoInteractions(summarizer, limiter);
    }

    @Test
    void 기록은_충분해도_태그가_하나도_없으면_거절한다() {
        given(profileService.getProfile(1L)).willReturn(
                new TasteProfileResponse(30, TODAY.minusDays(29), TODAY, 6, 5, true, List.of()));

        assertThatThrownBy(() -> service.summarize(1L)).isInstanceOf(TasteProfileNotEnoughException.class);
        verifyNoInteractions(summarizer, limiter);
    }

    @Test
    void 하루_한도를_넘으면_AI를_부르지_않는다() {
        given(profileService.getProfile(1L)).willReturn(eligible());
        given(limiter.acquire(1L)).willThrow(new TasteProfileLimitExceededException());

        assertThatThrownBy(() -> service.summarize(1L)).isInstanceOf(TasteProfileLimitExceededException.class);
        verifyNoInteractions(summarizer);
    }

    @Test
    void AI_호출이_실패하면_횟수를_돌려주고_503으로_알린다() {
        given(profileService.getProfile(1L)).willReturn(eligible());
        given(limiter.acquire(1L)).willReturn(TODAY);
        given(summarizer.summarize(any())).willThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.summarize(1L)).isInstanceOf(TasteProfileSummaryFailedException.class);
        verify(limiter).release(1L, TODAY);
    }

    @Test
    void 비었거나_너무_긴_답은_실패로_보고_횟수를_돌려준다() {
        given(profileService.getProfile(1L)).willReturn(eligible());
        given(limiter.acquire(1L)).willReturn(TODAY);
        given(summarizer.summarize(any()))
                .willReturn(new TasteProfileSummary("", "설명"))
                .willReturn(new TasteProfileSummary("가".repeat(21), "설명"))
                .willReturn(new TasteProfileSummary("유형", "가".repeat(101)))
                .willReturn(null);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.summarize(1L)).isInstanceOf(TasteProfileSummaryFailedException.class);
        }
        verify(limiter, org.mockito.Mockito.times(4)).release(1L, TODAY);
    }
}

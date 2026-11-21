package com.itta.toggleme.item.service;

import com.itta.toggleme.item.domain.ItemVisibility;
import com.itta.toggleme.item.dto.TasteProfileResponse;
import com.itta.toggleme.item.dto.TasteTagResponse;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.item.repository.ItemTagRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 취향 카드: 최근 30일(등록일 기준, 오늘 포함)의 공개 기록 태그를 가구 구분 없이 합쳐 센다.
 * 방에서 꺼진 가구의 기록도 포함한다. 공개 기록이 5개 미만이면 오류가 아니라 eligible=false로 알린다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteProfileService {

    static final int PERIOD_DAYS = 30;
    static final int REQUIRED_COUNT = 5;
    static final int MAX_TAGS = 12;
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;

    public TasteProfileResponse getProfile(Long memberId) {
        return getProfile(memberId, LocalDate.now(SERVICE_ZONE));
    }

    TasteProfileResponse getProfile(Long memberId, LocalDate today) {
        LocalDate from = today.minusDays(PERIOD_DAYS - 1);
        long itemCount = itemRepository.countByMemberIdAndVisibilityAndRecordedOnBetween(
                memberId, ItemVisibility.PUBLIC, from, today);
        boolean eligible = itemCount >= REQUIRED_COUNT;
        List<TasteTagResponse> tags = eligible
                ? itemTagRepository.findTasteTagCounts(
                                memberId, ItemVisibility.PUBLIC, from, today, Pageable.ofSize(MAX_TAGS))
                        .stream().map(TasteTagResponse::from).toList()
                : List.of();
        return new TasteProfileResponse(PERIOD_DAYS, from, today, itemCount, REQUIRED_COUNT, eligible, tags);
    }
}

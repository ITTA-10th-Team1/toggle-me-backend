package com.itta.toggleme.dashboard.service;

import com.itta.toggleme.dashboard.dto.DashboardResponse;
import com.itta.toggleme.follow.repository.FollowRepository;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final ItemRepository itemRepository;
    private final FollowRepository followRepository;

    public DashboardResponse getDashboard(int days) {
        LocalDate firstDate = LocalDate.now(KST).minusDays(days - 1L);
        Instant from = firstDate.atStartOfDay(KST).toInstant();
        long[] members = countByDate(firstDate, days, memberRepository.findCreatedAtSince(from));
        long[] items = countByDate(firstDate, days, itemRepository.findCreatedAtSince(from));
        List<DashboardResponse.Daily> daily = new ArrayList<>(days);

        for (int i = 0; i < days; i++) {
            daily.add(new DashboardResponse.Daily(firstDate.plusDays(i), members[i], items[i]));
        }

        return new DashboardResponse(
                Instant.now(),
                days,
                new DashboardResponse.Summary(
                        memberRepository.count(),
                        roomRepository.count(),
                        itemRepository.count(),
                        followRepository.count()
                ),
                daily
        );
    }

    // shortcut: 기간 내 생성 시각만 읽어 묶는다. 일별 데이터가 수만 건을 넘으면 DB GROUP BY로 바꾼다.
    private static long[] countByDate(LocalDate firstDate, int days, List<Instant> createdAtList) {
        long[] counts = new long[days];
        for (Instant createdAt : createdAtList) {
            long index = createdAt.atZone(KST).toLocalDate().toEpochDay() - firstDate.toEpochDay();
            if (index >= 0 && index < days) {
                counts[(int) index]++;
            }
        }
        return counts;
    }
}

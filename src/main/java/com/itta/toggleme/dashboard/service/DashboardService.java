package com.itta.toggleme.dashboard.service;

import com.itta.toggleme.dashboard.dto.DashboardResponse;
import com.itta.toggleme.follow.repository.FollowRepository;
import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import java.time.Duration;
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
    private final ImageAnalysisRepository imageAnalysisRepository;

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
                        // 탈퇴 회원은 빼고 센다. 일별 신규 가입 수는 이후 탈퇴 여부와 상관없이 가입한 날에 센다
                        memberRepository.countByStatus(MemberStatus.ACTIVE),
                        roomRepository.count(),
                        itemRepository.count(),
                        followRepository.count()
                ),
                funnel(from),
                analysis(from, Instant.now()),
                itemRepository.countByFurnitureTypeSince(from).stream()
                        .map(row -> new DashboardResponse.FurnitureItems(row.getCode(), row.getName(), row.getItems()))
                        .toList(),
                daily
        );
    }

    private DashboardResponse.Funnel funnel(Instant from) {
        List<Long> recordedDays = itemRepository.countRecordedDaysPerMemberJoinedSince(from, MemberStatus.ACTIVE);
        return new DashboardResponse.Funnel(
                memberRepository.countByStatusAndCreatedAtGreaterThanEqual(MemberStatus.ACTIVE, from),
                memberRepository.countByStatusAndProfileCompletedTrueAndCreatedAtGreaterThanEqual(MemberStatus.ACTIVE, from),
                recordedDays.size(),
                recordedDays.stream().filter(days -> days >= 2).count()
        );
    }

    // shortcut: 기간 내 분석의 상태·시각만 읽어 묶는다. 수만 건을 넘으면 DB 집계로 바꾼다. 경계 테스트를 위해 now를 받는다
    DashboardResponse.Analysis analysis(Instant from, Instant now) {
        Instant pendingSince = ImageAnalysis.pendingSince(now);
        long completed = 0;
        long failed = 0;
        long pending = 0;
        long stuck = 0;
        double totalSeconds = 0;
        double maxSeconds = 0;
        List<ImageAnalysisRepository.Timing> timings = imageAnalysisRepository.findTimingsSince(from);

        for (ImageAnalysisRepository.Timing timing : timings) {
            if (timing.getStatus() == ImageAnalysisStatus.COMPLETED) {
                double seconds = Duration.between(timing.getCreatedAt(), timing.getUpdatedAt()).toMillis() / 1000.0;
                completed++;
                totalSeconds += seconds;
                maxSeconds = Math.max(maxSeconds, seconds);
            } else if (timing.getStatus() == ImageAnalysisStatus.FAILED) {
                failed++;
            } else if (!timing.getCreatedAt().isBefore(pendingSince)) {
                // ImageAnalysis.currentStatus와 같은 경계: 60초를 넘겨야 멈춤이다
                pending++;
            } else {
                stuck++;
            }
        }

        return new DashboardResponse.Analysis(
                timings.size(), completed, failed, pending, stuck,
                completed == 0 ? null : totalSeconds / completed,
                completed == 0 ? null : maxSeconds
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

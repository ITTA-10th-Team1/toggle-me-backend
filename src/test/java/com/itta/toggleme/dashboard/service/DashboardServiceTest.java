package com.itta.toggleme.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itta.toggleme.follow.repository.FollowRepository;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final ImageAnalysisRepository imageAnalysisRepository = mock(ImageAnalysisRepository.class);
    private final DashboardService dashboardService = new DashboardService(
            memberRepository, roomRepository, itemRepository, followRepository, imageAnalysisRepository
    );

    @Test
    void 탈퇴_회원을_뺀_전체_수와_한국_날짜별_신규_회원과_기록을_집계한다() {
        LocalDate today = LocalDate.now(KST);

        when(memberRepository.countByStatus(MemberStatus.ACTIVE)).thenReturn(12L);
        when(roomRepository.count()).thenReturn(9L);
        when(itemRepository.count()).thenReturn(34L);
        when(followRepository.count()).thenReturn(7L);
        when(memberRepository.findCreatedAtSince(any())).thenReturn(List.of(
                today.minusDays(1).atTime(23, 30).atZone(KST).toInstant(),
                today.atTime(0, 30).atZone(KST).toInstant()
        ));
        when(itemRepository.findCreatedAtSince(any())).thenReturn(List.of(
                today.atTime(12, 0).atZone(KST).toInstant()
        ));

        var response = dashboardService.getDashboard(2);

        assertThat(response.summary()).extracting("members", "rooms", "items", "follows")
                .containsExactly(12L, 9L, 34L, 7L);
        assertThat(response.daily()).extracting("date", "members", "items")
                .containsExactly(
                        tuple(today.minusDays(1), 1L, 0L),
                        tuple(today, 1L, 1L)
                );
    }

    @Test
    void 퍼널은_기록한_날짜가_2일_이상인_회원을_재방문으로_센다() {
        when(memberRepository.countByStatusAndCreatedAtGreaterThanEqual(eq(MemberStatus.ACTIVE), any())).thenReturn(5L);
        when(memberRepository.countByStatusAndProfileCompletedTrueAndCreatedAtGreaterThanEqual(eq(MemberStatus.ACTIVE), any()))
                .thenReturn(4L);
        when(itemRepository.countRecordedDaysPerMemberJoinedSince(any(), eq(MemberStatus.ACTIVE)))
                .thenReturn(List.of(1L, 2L, 3L));

        var funnel = dashboardService.getDashboard(7).funnel();

        assertThat(funnel).extracting("signedUp", "profileCompleted", "recorded", "returned")
                .containsExactly(5L, 4L, 3L, 2L);
    }

    @Test
    void AI_분석은_60초가_지난_PENDING을_멈춤으로_세고_완료된_건만_소요_시간을_잰다() {
        Instant now = Instant.now();
        when(imageAnalysisRepository.findTimingsSince(any())).thenReturn(List.of(
                timing(ImageAnalysisStatus.COMPLETED, now.minusSeconds(100), now.minusSeconds(90)),
                timing(ImageAnalysisStatus.COMPLETED, now.minusSeconds(100), now.minusSeconds(70)),
                timing(ImageAnalysisStatus.FAILED, now.minusSeconds(100), now.minusSeconds(40)),
                timing(ImageAnalysisStatus.PENDING, now.minusSeconds(5), now.minusSeconds(5)),
                timing(ImageAnalysisStatus.PENDING, now.minusSeconds(120), now.minusSeconds(120))
        ));

        var analysis = dashboardService.getDashboard(7).imageAnalysis();

        assertThat(analysis).extracting("total", "completed", "failed", "pending", "stuck", "avgSeconds", "maxSeconds")
                .containsExactly(5L, 2L, 1L, 1L, 1L, 20.0, 30.0);
    }

    @Test
    void 정확히_60초_된_PENDING은_아직_진행_중으로_센다() {
        Instant now = Instant.parse("2026-11-18T00:00:00Z");
        when(imageAnalysisRepository.findTimingsSince(any())).thenReturn(List.of(
                timing(ImageAnalysisStatus.PENDING, now.minusSeconds(60), now.minusSeconds(60)),
                timing(ImageAnalysisStatus.PENDING, now.minusSeconds(61), now.minusSeconds(61))
        ));

        var analysis = dashboardService.analysis(Instant.EPOCH, now);

        assertThat(analysis).extracting("pending", "stuck").containsExactly(1L, 1L);
    }

    @Test
    void 완료된_분석이_없으면_소요_시간은_비어_있다() {
        var analysis = dashboardService.getDashboard(7).imageAnalysis();

        assertThat(analysis.total()).isZero();
        assertThat(analysis.avgSeconds()).isNull();
        assertThat(analysis.maxSeconds()).isNull();
    }

    private static ImageAnalysisRepository.Timing timing(ImageAnalysisStatus status, Instant createdAt, Instant updatedAt) {
        return new ImageAnalysisRepository.Timing() {
            public ImageAnalysisStatus getStatus() {
                return status;
            }

            public Instant getCreatedAt() {
                return createdAt;
            }

            public Instant getUpdatedAt() {
                return updatedAt;
            }
        };
    }
}

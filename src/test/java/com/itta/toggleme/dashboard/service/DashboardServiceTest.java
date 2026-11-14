package com.itta.toggleme.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itta.toggleme.follow.repository.FollowRepository;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    void 전체_수와_한국_날짜별_신규_회원과_기록을_집계한다() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        ItemRepository itemRepository = mock(ItemRepository.class);
        FollowRepository followRepository = mock(FollowRepository.class);
        LocalDate today = LocalDate.now(KST);

        when(memberRepository.count()).thenReturn(12L);
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

        var response = new DashboardService(
                memberRepository, roomRepository, itemRepository, followRepository
        ).getDashboard(2);

        assertThat(response.summary()).extracting("members", "rooms", "items", "follows")
                .containsExactly(12L, 9L, 34L, 7L);
        assertThat(response.daily()).extracting("date", "members", "items")
                .containsExactly(
                        tuple(today.minusDays(1), 1L, 0L),
                        tuple(today, 1L, 1L)
                );
    }
}

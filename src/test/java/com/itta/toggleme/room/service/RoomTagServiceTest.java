package com.itta.toggleme.room.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itta.toggleme.item.repository.RoomTagCandidate;
import com.itta.toggleme.room.dto.RoomTagResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomTagServiceTest {

    private static final long DESK = 1L;
    private static final long TV = 2L;
    private static final long VANITY = 3L;

    private final List<RoomTagCandidate> candidates = new ArrayList<>();
    private long nextItemId = 1;

    @Test
    void 가구_안에서는_많이_붙은_태그를_고른다() {
        item(DESK, "2026-10-28", "추리", "외국소설");
        item(DESK, "2026-10-30", "추리", "스릴러");

        assertThat(select()).containsExactly(new RoomTagResponse(DESK, "추리"));
    }

    @Test
    void 개수가_같으면_최근_아이템에_붙은_태그를_고른다() {
        item(DESK, "2026-10-28", "추리", "외국소설", "히가시노게이고");
        item(DESK, "2026-10-30", "추리", "한국소설", "스릴러");
        item(DESK, "2026-10-31", "에세이", "한국소설", "힐링");

        assertThat(select()).containsExactly(new RoomTagResponse(DESK, "한국소설"));
    }

    @Test
    void 아이템이_하나면_앞_순서_태그를_고른다() {
        item(VANITY, "2026-10-31", "데일리", "캐주얼", "봄");

        assertThat(select()).containsExactly(new RoomTagResponse(VANITY, "데일리"));
    }

    @Test
    void 최근에_기록한_가구부터_고르고_이미_뽑힌_태그는_건너뛴다() {
        item(DESK, "2026-10-31", "추리", "한국소설");
        item(TV, "2026-11-01", "추리", "스릴러");
        item(VANITY, "2026-10-20", "데일리");

        assertThat(select()).containsExactly(
                new RoomTagResponse(TV, "추리"),
                new RoomTagResponse(DESK, "한국소설"),
                new RoomTagResponse(VANITY, "데일리"));
    }

    @Test
    void 고를_태그가_남지_않은_가구는_빠진다() {
        item(TV, "2026-11-01", "추리");
        item(DESK, "2026-10-31", "추리");

        assertThat(select()).containsExactly(new RoomTagResponse(TV, "추리"));
    }

    @Test
    void 태그가_없으면_빈_목록이다() {
        assertThat(select()).isEmpty();
    }

    private void item(long furnitureTypeId, String date, String... tags) {
        long itemId = nextItemId++;
        Instant createdAt = Instant.parse(date + "T00:00:00Z");
        for (int position = 0; position < tags.length; position++) {
            candidates.add(new RoomTagCandidate(furnitureTypeId, tags[position], position, itemId, createdAt));
        }
    }

    private List<RoomTagResponse> select() {
        return RoomTagService.selectRoomTags(candidates);
    }
}

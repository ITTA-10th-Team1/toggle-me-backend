package com.itta.toggleme.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OnboardingConcurrencyIntegrationTest {

    private static final Duration WAIT_OBSERVATION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration STABLE_WAIT_DURATION = Duration.ofMillis(300);

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private MemberRepository memberRepository;
    @Autowired private FurnitureTypeRepository furnitureTypeRepository;
    @Autowired private FurnitureVariantRepository furnitureVariantRepository;
    @Autowired private RoomStyleRepository roomStyleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    private Long memberId;
    private final List<Long> furnitureTypeIds = new ArrayList<>();
    private final List<Long> furnitureVariantIds = new ArrayList<>();
    private final List<Long> roomStyleIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        if (memberId != null) {
            jdbcTemplate.update("delete from room_furniture where floor_id in "
                            + "(select id from room_floor where room_id in "
                            + "(select id from room where owner_id = ?))",
                    memberId);
            jdbcTemplate.update(
                    "delete from room_floor where room_id in (select id from room where owner_id = ?)", memberId);
            jdbcTemplate.update("delete from room where owner_id = ?", memberId);
        }
        furnitureVariantIds.forEach(id -> jdbcTemplate.update("delete from furniture_variant where id = ?", id));
        furnitureTypeIds.forEach(id -> jdbcTemplate.update("delete from furniture_type where id = ?", id));
        roomStyleIds.forEach(id -> jdbcTemplate.update("delete from room_style where id = ?", id));
        if (memberId != null) {
            jdbcTemplate.update("delete from member where id = ?", memberId);
        }
    }

    @Test
    void 동시_온보딩_요청은_같은_방으로_완료된다() throws Exception {
        createFixtures();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            long blockerConnectionId = connectionId(blocker);
            lockVariant(blocker, furnitureVariantIds.getFirst());

            List<Future<MockHttpServletResponse>> responses = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                responses.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return completeOnboarding();
                }));
            }

            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                assertThat(waitUntilBothRequestsAreWaiting(blockerConnectionId)).isTrue();
            } finally {
                blocker.rollback();
            }

            List<MockHttpServletResponse> results = List.of(
                    responses.get(0).get(30, TimeUnit.SECONDS),
                    responses.get(1).get(30, TimeUnit.SECONDS)
            );
            assertThat(results).extracting(MockHttpServletResponse::getStatus).containsOnly(200);
            long roomId = resultsRoomId(results.getFirst());
            assertThat(resultsRoomId(results.getLast())).isEqualTo(roomId);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from room where owner_id = ?", Long.class, memberId))
                    .isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("select count(*) from room_floor where room_id = ?", Long.class,
                    roomId)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("select count(*) from room_furniture where floor_id in "
                    + "(select id from room_floor where room_id = ?)", Long.class, roomId))
                    .isEqualTo(furnitureVariantIds.size());
            assertThat(jdbcTemplate.queryForObject(
                    "select onboarding_completed from member where id = ?", Boolean.class, memberId)).isTrue();
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void createFixtures() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Member member = Member.create("user_" + suffix.substring(0, 8), "테스트");
        member.updateProfile("race_" + suffix.substring(0, 12), "테스트");
        memberId = memberRepository.save(member).getId();

        roomStyleIds.add(roomStyleRepository.save(
                RoomStyle.create(RoomStyleType.WALL, "테스트 벽", "test/wall/" + suffix)).getId());
        roomStyleIds.add(roomStyleRepository.save(
                RoomStyle.create(RoomStyleType.FLOOR, "테스트 바닥", "test/floor/" + suffix)).getId());

        for (int i = 0; i < 4; i++) {
            FurnitureType type = FurnitureType.create("TEST_" + suffix + "_" + i, "테스트 가구 " + i, i);
            type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
            furnitureTypeIds.add(furnitureTypeRepository.save(type).getId());
            furnitureVariantIds.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(
                            type, "테스트 디자인 " + i, "test/furniture/" + suffix + "/" + i, 1, 1)
            ).getId());
        }
    }

    private MockHttpServletResponse completeOnboarding() throws Exception {
        String ids = furnitureVariantIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return mockMvc.perform(post("/api/v1/members/onboarding")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furnitureVariantIds\":[" + ids + "],\"visibility\":\"SOCIAL_OFF\"}"))
                .andReturn().getResponse();
    }

    private long connectionId(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select connection_id()");
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private void lockVariant(Connection connection, Long variantId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select id from furniture_variant where id = ? for update")) {
            statement.setLong(1, variantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
            }
        }
    }

    private boolean waitUntilBothRequestsAreWaiting(long blockerConnectionId) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT_OBSERVATION_TIMEOUT.toNanos();
        Set<Long> stableIds = Set.of();
        long stableSince = 0;

        while (System.nanoTime() < deadline) {
            Set<Long> queryIds = new HashSet<>(jdbcTemplate.queryForList("""
                    select id
                    from information_schema.processlist
                    where id <> ?
                      and id <> connection_id()
                      and user = substring_index(current_user(), '@', 1)
                      and db = database()
                      and command = 'Query'
                      and info is not null
                    """, Long.class, blockerConnectionId));
            if (queryIds.size() >= 2 && queryIds.equals(stableIds)) {
                if (System.nanoTime() - stableSince >= STABLE_WAIT_DURATION.toNanos()) {
                    return true;
                }
            } else if (queryIds.size() >= 2) {
                stableIds = queryIds;
                stableSince = System.nanoTime();
            } else {
                stableIds = Set.of();
            }
            Thread.sleep(25);
        }
        return false;
    }

    private long resultsRoomId(MockHttpServletResponse response) throws Exception {
        return ((Number) JsonPath.read(response.getContentAsString(), "$.roomId")).longValue();
    }
}

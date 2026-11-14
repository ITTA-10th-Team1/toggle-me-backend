package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.itta.toggleme.auth.client.KakaoClient;
import com.itta.toggleme.auth.client.KakaoUserInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 카카오 첫 로그인이 동시에 들어오는 경우를 실제 DB로 확인한다(트랜잭션 없이 실행). */
@SpringBootTest
@AutoConfigureMockMvc
class SocialLoginConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 6;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KakaoClient kakaoClient;

    private final List<String> createdProviderUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String providerUserId : createdProviderUserIds) {
            List<Long> memberIds = jdbcTemplate.queryForList(
                    "select member_id from social_account where provider_user_id = ?", Long.class, providerUserId);
            for (Long memberId : memberIds) {
                jdbcTemplate.update("delete from refresh_token where member_id = ?", memberId);
                jdbcTemplate.update("delete from social_account where member_id = ?", memberId);
                jdbcTemplate.update("delete from member where id = ?", memberId);
            }
        }
        createdProviderUserIds.clear();
    }

    @RepeatedTest(5)
    void 같은_카카오_계정의_첫_로그인이_동시에_들어와도_모두_성공하고_회원은_한_명만_생긴다() throws Exception {
        String kakaoId = newProviderUserId();
        given(kakaoClient.getUserInfo(anyString())).willReturn(new KakaoUserInfo(kakaoId, "닉네임"));

        List<Integer> statuses = runConcurrently(() -> send("/api/v1/auth/oauth/kakao", "{\"accessToken\":\"t\"}"));

        assertThat(statuses).hasSize(CONCURRENT_REQUESTS).containsOnly(200);
        assertThat(countAccounts("KAKAO", kakaoId)).isEqualTo(1);
    }

    private int send(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getStatus();
    }

    private List<Integer> runConcurrently(Callable<Integer> task) throws Exception {
        List<Callable<Integer>> tasks = Collections.nCopies(CONCURRENT_REQUESTS, task);
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> each : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return each.call();
                }));
            }
            ready.await();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    private long countAccounts(String provider, String providerUserId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from social_account where provider = ? and provider_user_id = ?",
                Long.class, provider, providerUserId);
    }

    private String newProviderUserId() {
        String id = UUID.randomUUID().toString().substring(0, 18);
        createdProviderUserIds.add(id);
        return id;
    }
}

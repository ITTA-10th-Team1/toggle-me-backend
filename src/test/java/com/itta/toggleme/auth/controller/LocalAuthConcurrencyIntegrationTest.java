package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LocalAuthConcurrencyIntegrationTest {

    private static final String SIGN_UP_URL = "/api/v1/auth/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PASSWORD = "password123";
    private static final int CONCURRENT_REQUESTS = 4;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String email : createdEmails) {
            List<Long> memberIds = jdbcTemplate.queryForList(
                    "select member_id from local_account where email = ?", Long.class, email);
            for (Long memberId : memberIds) {
                jdbcTemplate.update("delete from refresh_token where member_id = ?", memberId);
                jdbcTemplate.update("delete from local_account where member_id = ?", memberId);
                jdbcTemplate.update("delete from member where id = ?", memberId);
            }
        }
        createdEmails.clear();
    }

    @Test
    void 같은_이메일로_동시에_가입하면_한_건만_성공하고_나머지는_409이며_고아_회원이_남지_않는다() throws Exception {
        String email = newEmail();
        long membersBefore = countMembers();

        List<Integer> statuses = runConcurrently(CONCURRENT_REQUESTS, () -> signUp(email));

        assertThat(statuses).hasSize(CONCURRENT_REQUESTS).containsOnly(201, 409);
        assertThat(statuses.stream().filter(status -> status == 201)).hasSize(1);
        assertThat(countAccounts(email)).isEqualTo(1);
        assertThat(countMembers() - membersBefore).isEqualTo(1);
    }

    @Test
    void 서로_다른_이메일로_동시에_가입하면_모두_성공한다() throws Exception {
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            emails.add(newEmail());
        }
        List<Callable<Integer>> tasks = emails.stream()
                .<Callable<Integer>>map(email -> () -> signUp(email))
                .toList();

        List<Integer> statuses = runConcurrently(tasks);

        assertThat(statuses).hasSize(CONCURRENT_REQUESTS).containsOnly(201);
    }

    @Test
    void 같은_계정으로_동시에_로그인해도_모두_성공한다() throws Exception {
        String email = newEmail();
        assertThat(signUp(email)).isEqualTo(201);

        List<Integer> statuses = runConcurrently(CONCURRENT_REQUESTS, () -> login(email));

        assertThat(statuses).hasSize(CONCURRENT_REQUESTS).containsOnly(200);
    }

    private int signUp(String email) throws Exception {
        return mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andReturn().getResponse().getStatus();
    }

    private int login(String email) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andReturn().getResponse().getStatus();
    }

    private List<Integer> runConcurrently(int count, Callable<Integer> task) throws Exception {
        return runConcurrently(java.util.Collections.nCopies(count, task));
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
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

    private String newEmail() {
        String email = "race-" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private long countMembers() {
        return jdbcTemplate.queryForObject("select count(*) from member", Long.class);
    }

    private long countAccounts(String email) {
        return jdbcTemplate.queryForObject("select count(*) from local_account where email = ?", Long.class, email);
    }
}

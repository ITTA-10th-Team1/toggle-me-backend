package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
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

    private final List<String> createdLoginIds = new ArrayList<>();

    private record Outcome(int status, String code) {
    }

    @AfterEach
    void cleanUp() {
        for (String loginId : createdLoginIds) {
            List<Long> memberIds = jdbcTemplate.queryForList(
                    "select member_id from local_account where login_id = ?", Long.class, loginId);
            for (Long memberId : memberIds) {
                jdbcTemplate.update("delete from refresh_token where member_id = ?", memberId);
                jdbcTemplate.update("delete from local_account where member_id = ?", memberId);
                jdbcTemplate.update("delete from member where id = ?", memberId);
            }
        }
        createdLoginIds.clear();
    }

    @Test
    void 같은_아이디와_이메일로_동시에_가입하면_한_건만_성공하고_나머지는_409이며_고아_회원이_남지_않는다() throws Exception {
        String loginId = newLoginId();
        String email = newEmail();
        long membersBefore = countMembers();

        List<Outcome> outcomes = runConcurrently(CONCURRENT_REQUESTS, () -> signUp(loginId, email));

        assertThat(statuses(outcomes)).hasSize(CONCURRENT_REQUESTS).containsOnly(201, 409);
        assertThat(statuses(outcomes).stream().filter(status -> status == 201)).hasSize(1);
        assertThat(countAccounts(loginId)).isEqualTo(1);
        assertThat(countMembers() - membersBefore).isEqualTo(1);
    }

    @Test
    void 같은_아이디와_다른_이메일로_동시에_가입하면_진_요청은_아이디_중복_코드를_받는다() throws Exception {
        String loginId = newLoginId();
        long membersBefore = countMembers();
        List<Callable<Outcome>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String email = newEmail();
            tasks.add(() -> signUp(loginId, email));
        }

        List<Outcome> outcomes = runConcurrently(tasks);

        assertThat(statuses(outcomes).stream().filter(status -> status == 201)).hasSize(1);
        assertThat(losers(outcomes)).hasSize(CONCURRENT_REQUESTS - 1)
                .allSatisfy(outcome -> {
                    assertThat(outcome.status()).isEqualTo(409);
                    assertThat(outcome.code()).isEqualTo("LOGIN_ID_ALREADY_EXISTS");
                });
        assertThat(countMembers() - membersBefore).isEqualTo(1);
    }

    @Test
    void 다른_아이디와_같은_이메일로_동시에_가입하면_진_요청은_이메일_중복_코드를_받는다() throws Exception {
        String email = newEmail();
        long membersBefore = countMembers();
        List<Callable<Outcome>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String loginId = newLoginId();
            tasks.add(() -> signUp(loginId, email));
        }

        List<Outcome> outcomes = runConcurrently(tasks);

        assertThat(statuses(outcomes).stream().filter(status -> status == 201)).hasSize(1);
        assertThat(losers(outcomes)).hasSize(CONCURRENT_REQUESTS - 1)
                .allSatisfy(outcome -> {
                    assertThat(outcome.status()).isEqualTo(409);
                    assertThat(outcome.code()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });
        assertThat(countMembers() - membersBefore).isEqualTo(1);
    }

    @Test
    void 서로_다른_아이디와_이메일로_동시에_가입하면_모두_성공한다() throws Exception {
        List<Callable<Outcome>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String loginId = newLoginId();
            String email = newEmail();
            tasks.add(() -> signUp(loginId, email));
        }

        List<Outcome> outcomes = runConcurrently(tasks);

        assertThat(statuses(outcomes)).hasSize(CONCURRENT_REQUESTS).containsOnly(201);
    }

    @Test
    void 같은_계정으로_동시에_로그인해도_모두_성공한다() throws Exception {
        String loginId = newLoginId();
        assertThat(signUp(loginId, newEmail()).status()).isEqualTo(201);

        List<Outcome> outcomes = runConcurrently(CONCURRENT_REQUESTS, () -> login(loginId));

        assertThat(statuses(outcomes)).hasSize(CONCURRENT_REQUESTS).containsOnly(200);
    }

    @Test
    void 동시_중복_가입이_겹쳐도_서버_로그에_이메일과_아이디가_남지_않는다() throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        List<String> sensitiveValues = new ArrayList<>();
        try {
            for (int round = 0; round < 2; round++) {
                String sameLoginId = newLoginId();
                sensitiveValues.add(sameLoginId);
                List<Callable<Outcome>> sameLoginIdTasks = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    String email = newEmail();
                    sensitiveValues.add(email);
                    sameLoginIdTasks.add(() -> signUp(sameLoginId, email));
                }
                runConcurrently(sameLoginIdTasks);

                String sameEmail = newEmail();
                sensitiveValues.add(sameEmail);
                List<Callable<Outcome>> sameEmailTasks = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    String loginId = newLoginId();
                    sensitiveValues.add(loginId);
                    sameEmailTasks.add(() -> signUp(loginId, sameEmail));
                }
                runConcurrently(sameEmailTasks);
            }
        } finally {
            rootLogger.detachAppender(appender);
        }

        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> sensitiveValues.stream().anyMatch(message::contains));
    }

    private Outcome signUp(String loginId, String email) throws Exception {
        return send(SIGN_UP_URL, "{\"loginId\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(loginId, email, PASSWORD));
    }

    private Outcome login(String loginId) throws Exception {
        return send(LOGIN_URL, "{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(loginId, PASSWORD));
    }

    private Outcome send(String url, String body) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse();
        String code = null;
        if (response.getStatus() == 409) {
            code = JsonPath.read(response.getContentAsString(StandardCharsets.UTF_8), "$.code");
        }
        return new Outcome(response.getStatus(), code);
    }

    private List<Outcome> runConcurrently(int count, Callable<Outcome> task) throws Exception {
        return runConcurrently(java.util.Collections.nCopies(count, task));
    }

    private List<Outcome> runConcurrently(List<Callable<Outcome>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (Callable<Outcome> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private List<Integer> statuses(List<Outcome> outcomes) {
        return outcomes.stream().map(Outcome::status).toList();
    }

    private List<Outcome> losers(List<Outcome> outcomes) {
        return outcomes.stream().filter(outcome -> outcome.status() != 201).toList();
    }

    private String newLoginId() {
        String loginId = "r" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        createdLoginIds.add(loginId);
        return loginId;
    }

    private String newEmail() {
        return "race-" + UUID.randomUUID() + "@example.com";
    }

    private long countMembers() {
        return jdbcTemplate.queryForObject("select count(*) from member", Long.class);
    }

    private long countAccounts(String loginId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from local_account where login_id = ?", Long.class, loginId);
    }
}

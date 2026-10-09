package com.itta.toggleme.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itta.toggleme.auth.exception.InvalidRefreshTokenException;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.service.MemberService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TokenReissueConcurrencyTest {

    private static final int REQUEST_COUNT = 2;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long memberId;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from refresh_token where member_id = ?", memberId);
        jdbcTemplate.update("delete from member where id = ?", memberId);
    }

    @Test
    void 같은_Refresh_Token으로_동시에_갱신하면_하나만_성공한다() throws Exception {
        Member member = memberService.register("동시성");
        memberId = member.getId();
        String refreshToken = tokenService.issue(member).refreshToken();

        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        CountDownLatch ready = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < REQUEST_COUNT; i++) {
            Callable<Boolean> reissue = () -> {
                ready.await();
                try {
                    tokenService.reissue(refreshToken);
                    return true;
                } catch (InvalidRefreshTokenException e) {
                    return false;
                }
            };
            results.add(executor.submit(reissue));
        }
        ready.countDown();

        int successCount = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successCount++;
            }
        }
        executor.shutdown();

        assertThat(successCount).isEqualTo(1);
    }
}

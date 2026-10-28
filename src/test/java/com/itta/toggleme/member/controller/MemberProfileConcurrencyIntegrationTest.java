package com.itta.toggleme.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.global.security.JwtTokenProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class MemberProfileConcurrencyIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<Long> memberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long memberId : memberIds) {
            jdbcTemplate.update("delete from member where id = ?", memberId);
        }
    }

    @Test
    void 같은_핸들을_동시에_저장하면_한_건만_성공하고_나머지는_409다() throws Exception {
        List<Member> members = List.of(saveMember(), saveMember());
        String handle = "race_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Member member : members) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    MvcResult result = mockMvc.perform(patch("/api/v1/members/me/profile")
                                    .header("Authorization", "Bearer "
                                            + jwtTokenProvider.createAccessToken(member.getId()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"handle\":\"" + handle + "\",\"nickname\":\"테스트\"}"))
                            .andReturn();
                    return result.getResponse().getStatus();
                }));
            }
            ready.await();
            start.countDown();
            List<Integer> statuses = List.of(results.get(0).get(30, TimeUnit.SECONDS),
                    results.get(1).get(30, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
    }

    private Member saveMember() {
        Member member = memberRepository.save(
                Member.create("user_" + UUID.randomUUID().toString().substring(0, 8), "테스트"));
        memberIds.add(member.getId());
        return member;
    }
}

package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.domain.RefreshToken;
import com.itta.toggleme.auth.repository.RefreshTokenRepository;
import com.itta.toggleme.auth.service.TokenService;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.SocialAccount;
import com.itta.toggleme.member.domain.SocialProvider;
import com.itta.toggleme.member.repository.SocialAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {"toss.callback-username=toss-user", "toss.callback-password=toss-pass"})
class TossCallbackTest {

    private static final String UNLINK_URL = "/api/v1/auth/oauth/toss/unlink";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void POST_콜백이_오면_해당_회원의_Refresh_Token을_모두_폐기한다() throws Exception {
        String userKey = randomUserKey();
        Member member = joinTossMember(userKey);
        tokenService.issue(member);
        tokenService.issue(member);

        mockMvc.perform(post(UNLINK_URL)
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "toss-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userKey\":\"" + userKey + "\",\"referrer\":\"UNLINK\"}"))
                .andExpect(status().isOk());

        assertThat(tokensOf(member)).hasSize(2).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void userKey가_숫자로_와도_처리한다() throws Exception {
        String userKey = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L));
        Member member = joinTossMember(userKey);
        tokenService.issue(member);

        mockMvc.perform(post(UNLINK_URL)
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "toss-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userKey\":" + userKey + ",\"referrer\":\"WITHDRAWAL_TOSS\"}"))
                .andExpect(status().isOk());

        assertThat(tokensOf(member)).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void GET_콜백도_처리한다() throws Exception {
        String userKey = randomUserKey();
        Member member = joinTossMember(userKey);
        tokenService.issue(member);

        mockMvc.perform(get(UNLINK_URL)
                        .param("userKey", userKey)
                        .param("referrer", "UNLINK")
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "toss-pass")))
                .andExpect(status().isOk());

        assertThat(tokensOf(member)).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void 다른_회원의_토큰은_폐기하지_않는다() throws Exception {
        String userKey = randomUserKey();
        joinTossMember(userKey);
        Member other = memberService.register(null);
        tokenService.issue(other);

        mockMvc.perform(get(UNLINK_URL)
                        .param("userKey", userKey)
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "toss-pass")))
                .andExpect(status().isOk());

        assertThat(tokensOf(other)).noneMatch(RefreshToken::isRevoked);
    }

    @Test
    void 모르는_userKey도_200으로_응답한다() throws Exception {
        mockMvc.perform(get(UNLINK_URL)
                        .param("userKey", randomUserKey())
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "toss-pass")))
                .andExpect(status().isOk());
    }

    @Test
    void 인증이_없거나_틀리면_401이고_토큰을_폐기하지_않는다() throws Exception {
        String userKey = randomUserKey();
        Member member = joinTossMember(userKey);
        tokenService.issue(member);

        mockMvc.perform(get(UNLINK_URL).param("userKey", userKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOSS_CALLBACK_UNAUTHORIZED"));
        mockMvc.perform(get(UNLINK_URL)
                        .param("userKey", userKey)
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "wrong")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(UNLINK_URL)
                        .param("userKey", userKey)
                        .header(HttpHeaders.AUTHORIZATION, "Basic !!!not-base64!!!"))
                .andExpect(status().isUnauthorized());

        assertThat(tokensOf(member)).noneMatch(RefreshToken::isRevoked);
    }

    @Test
    void userKey가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post(UNLINK_URL)
                        .header(HttpHeaders.AUTHORIZATION, basic("toss-user", "toss-pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referrer\":\"UNLINK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("userKey"));
    }

    private Member joinTossMember(String userKey) {
        Member member = memberService.register(null);
        socialAccountRepository.save(SocialAccount.connect(member, SocialProvider.TOSS, userKey));
        return member;
    }

    private List<RefreshToken> tokensOf(Member member) {
        return refreshTokenRepository.findAll().stream()
                .filter(token -> token.getMember().getId().equals(member.getId()))
                .toList();
    }

    private String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private String randomUserKey() {
        return UUID.randomUUID().toString().substring(0, 18);
    }
}

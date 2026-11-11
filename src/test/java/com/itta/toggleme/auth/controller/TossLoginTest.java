package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.client.TossClient;
import com.itta.toggleme.auth.client.TossUserInfo;
import com.itta.toggleme.auth.exception.TossApiException;
import com.itta.toggleme.auth.exception.TossAuthorizationCodeInvalidException;
import com.itta.toggleme.member.domain.SocialProvider;
import com.itta.toggleme.member.repository.SocialAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TossLoginTest {

    private static final String TOSS_LOGIN_URL = "/api/v1/auth/oauth/toss";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @MockitoBean
    private TossClient tossClient;

    @Test
    void 처음_로그인하면_회원가입_후_토큰을_발급한다() throws Exception {
        String userKey = randomUserKey();
        given(tossClient.getUserInfo(anyString(), anyString())).willReturn(new TossUserInfo(userKey));

        login()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.onboardingRequired").value(true))
                .andExpect(jsonPath("$.onboardingStep").value("PROFILE"));

        assertThat(socialAccountRepository.findWithMember(SocialProvider.TOSS, userKey))
                .hasValueSatisfying(account -> assertThat(account.getMember().getHandle()).startsWith("user_"));
    }

    @Test
    void 같은_userKey로_다시_로그인하면_같은_회원으로_로그인한다() throws Exception {
        String userKey = randomUserKey();
        given(tossClient.getUserInfo(anyString(), anyString())).willReturn(new TossUserInfo(userKey));
        login().andExpect(status().isOk());
        Long memberId = findMemberId(userKey);

        login().andExpect(status().isOk());

        assertThat(findMemberId(userKey)).isEqualTo(memberId);
    }

    @Test
    void 카카오와_같은_값의_userKey여도_별개_회원으로_가입한다() throws Exception {
        String sameValue = randomUserKey();
        given(tossClient.getUserInfo(anyString(), anyString())).willReturn(new TossUserInfo(sameValue));

        login().andExpect(status().isOk());

        assertThat(socialAccountRepository.findWithMember(SocialProvider.TOSS, sameValue)).isPresent();
        assertThat(socialAccountRepository.findWithMember(SocialProvider.KAKAO, sameValue)).isEmpty();
    }

    @Test
    void 인가_코드가_유효하지_않으면_401을_반환한다() throws Exception {
        given(tossClient.getUserInfo(anyString(), anyString())).willThrow(new TossAuthorizationCodeInvalidException());

        login()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOSS_AUTHORIZATION_CODE_INVALID"));
    }

    @Test
    void 토스_서버_오류면_502를_반환한다() throws Exception {
        given(tossClient.getUserInfo(anyString(), anyString())).willThrow(new TossApiException());

        login()
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("TOSS_API_ERROR"));
    }

    @Test
    void 인가_코드가_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post(TOSS_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationCode\":\"\",\"referrer\":\"DEFAULT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("authorizationCode"));
    }

    @Test
    void referrer가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post(TOSS_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationCode\":\"code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("referrer"));
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post(TOSS_LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"authorizationCode\":\"toss-code\",\"referrer\":\"SANDBOX\"}"));
    }

    private Long findMemberId(String userKey) {
        return socialAccountRepository.findWithMember(SocialProvider.TOSS, userKey)
                .orElseThrow()
                .getMember()
                .getId();
    }

    private String randomUserKey() {
        return UUID.randomUUID().toString().substring(0, 18);
    }
}

package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.client.KakaoClient;
import com.itta.toggleme.auth.client.KakaoUserInfo;
import com.itta.toggleme.auth.exception.InvalidKakaoTokenException;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KakaoLoginTest {

    private static final String KAKAO_LOGIN_URL = "/api/v1/auth/oauth/kakao";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @MockitoBean
    private KakaoClient kakaoClient;

    @Test
    void 처음_로그인하면_회원가입_후_토큰을_발급한다() throws Exception {
        String kakaoId = randomKakaoId();
        given(kakaoClient.getUserInfo(anyString())).willReturn(new KakaoUserInfo(kakaoId, "카카오닉네임"));

        mockMvc.perform(post(KAKAO_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"kakao-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.newMember").value(true));

        assertThat(socialAccountRepository.findWithMember(SocialProvider.KAKAO, kakaoId))
                .hasValueSatisfying(account -> {
                    assertThat(account.getMember().getNickname()).isEqualTo("카카오닉네임");
                    assertThat(account.getMember().getHandle()).startsWith("user_");
                });
    }

    @Test
    void 이미_가입한_회원이_로그인하면_기존_회원으로_토큰을_발급한다() throws Exception {
        String kakaoId = randomKakaoId();
        given(kakaoClient.getUserInfo(anyString())).willReturn(new KakaoUserInfo(kakaoId, "카카오닉네임"));
        mockMvc.perform(post(KAKAO_LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accessToken\":\"kakao-token\"}"));

        mockMvc.perform(post(KAKAO_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"kakao-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newMember").value(false));
    }

    @Test
    void 카카오_토큰이_유효하지_않으면_401을_반환한다() throws Exception {
        given(kakaoClient.getUserInfo(anyString())).willThrow(new InvalidKakaoTokenException());

        mockMvc.perform(post(KAKAO_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_KAKAO_TOKEN"));
    }

    @Test
    void 카카오_토큰이_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post(KAKAO_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("accessToken"));
    }

    private String randomKakaoId() {
        return UUID.randomUUID().toString().substring(0, 18);
    }
}

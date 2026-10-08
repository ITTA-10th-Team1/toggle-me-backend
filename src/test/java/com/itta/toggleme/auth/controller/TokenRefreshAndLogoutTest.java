package com.itta.toggleme.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.client.KakaoClient;
import com.itta.toggleme.auth.client.KakaoUserInfo;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class TokenRefreshAndLogoutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KakaoClient kakaoClient;

    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void login() throws Exception {
        String kakaoId = UUID.randomUUID().toString().substring(0, 18);
        given(kakaoClient.getUserInfo(anyString())).willReturn(new KakaoUserInfo(kakaoId, "토글러"));

        String body = mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"kakao-token\"}"))
                .andReturn().getResponse().getContentAsString();
        accessToken = JsonPath.read(body, "$.accessToken");
        refreshToken = JsonPath.read(body, "$.refreshToken");
    }

    @Test
    void Refresh_Token으로_새_토큰을_발급한다() throws Exception {
        refresh(refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void 한_번_사용한_Refresh_Token은_다시_사용할_수_없다() throws Exception {
        refresh(refreshToken).andExpect(status().isOk());

        refresh(refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 존재하지_않는_Refresh_Token이면_401을_반환한다() throws Exception {
        refresh("unknown-refresh-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 로그아웃하면_Refresh_Token을_더_이상_사용할_수_없다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(refreshToken)))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void Access_Token_없이_로그아웃하면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshTokenBody(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenBody(token)));
    }

    private String refreshTokenBody(String token) {
        return "{\"refreshToken\":\"" + token + "\"}";
    }
}

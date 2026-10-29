package com.itta.toggleme.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void 토큰_없이_보호된_API를_호출하면_401_공통_에러_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 잘못된_토큰으로_호출하면_401_공통_에러_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 발급한_Access_Token의_subject는_회원_ID다() {
        String accessToken = jwtTokenProvider.createAccessToken(42L);

        Jwt jwt = jwtDecoder.decode(accessToken);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }
}

package com.itta.toggleme.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.VerifiedEmailTokens;
import com.itta.toggleme.auth.repository.EmailVerificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginIdAvailabilityIntegrationTest {

    private static final String URL = "/api/v1/auth/login-id-availability";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void 아무도_쓰지_않는_아이디는_로그인_없이_확인하면_사용_가능이다() throws Exception {
        check(randomLoginId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 이미_가입된_아이디는_사용_불가다() throws Exception {
        String loginId = randomLoginId();
        signUp(loginId).andExpect(status().isCreated());

        check(loginId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "Toggle01", "toggle_01", "a23456789012345678901", " "})
    void 가입_아이디_규칙에_맞지_않으면_400이다(String loginId) throws Exception {
        check(loginId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 아이디를_보내지_않으면_400이다() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    private ResultActions check(String loginId) throws Exception {
        return mockMvc.perform(get(URL).param("loginId", loginId));
    }

    private ResultActions signUp(String loginId) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String token = VerifiedEmailTokens.issue(emailVerificationRepository, email);
        return mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"loginId\":\"%s\",\"email\":\"%s\",\"emailVerificationToken\":\"%s\","
                        + "\"password\":\"password123\"}").formatted(loginId, email, token)));
    }

    private String randomLoginId() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

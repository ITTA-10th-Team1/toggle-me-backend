package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LocalLoginIntegrationTest {

    private static final String SIGN_UP_URL = "/api/v1/auth/signup";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalAccountRepository localAccountRepository;

    @Test
    void 가입한_계정으로_로그인하면_토큰을_발급한다() throws Exception {
        String email = signUpNewMember(PASSWORD);

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.onboardingRequired").value(true));
    }

    @Test
    void 이메일의_대소문자와_앞뒤_공백은_무시하고_로그인한다() throws Exception {
        String email = signUpNewMember(PASSWORD);

        login("  " + email.toUpperCase() + " ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 온보딩을_마친_회원은_온보딩이_필요하지_않다() throws Exception {
        String email = signUpNewMember(PASSWORD);
        localAccountRepository.findWithMemberByEmail(email).orElseThrow().getMember().completeOnboarding();

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingRequired").value(false));
    }

    @Test
    void 비밀번호가_틀리면_401을_반환한다() throws Exception {
        String email = signUpNewMember(PASSWORD);

        login(email, "wrong-password1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENTIALS_INVALID"));
    }

    @Test
    void 가입되지_않은_이메일이면_401을_반환한다() throws Exception {
        login(randomEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENTIALS_INVALID"));
    }

    @Test
    void 비밀번호가_틀린_경우와_이메일이_없는_경우의_응답은_완전히_같다() throws Exception {
        String email = signUpNewMember(PASSWORD);

        String wrongPassword = responseBody(
                login(email, "wrong-password1").andExpect(status().isUnauthorized()));
        String unknownEmail = responseBody(
                login(randomEmail(), "wrong-password1").andExpect(status().isUnauthorized()));

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void 탈퇴한_회원은_비밀번호가_맞아도_로그인할_수_없다() throws Exception {
        String email = signUpNewMember(PASSWORD);
        LocalAccount account = localAccountRepository.findWithMemberByEmail(email).orElseThrow();
        ReflectionTestUtils.setField(account.getMember(), "status", MemberStatus.WITHDRAWN);

        login(email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENTIALS_INVALID"));
    }

    @Test
    void 이메일과_비밀번호가_비어_있으면_400을_반환한다() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("loginId", "password")));
    }

    @Test
    void 필드가_없는_요청은_400을_반환한다() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("loginId", "password")));
    }

    @Test
    void 비밀번호가_72바이트를_넘으면_500이_아니라_400을_반환한다() throws Exception {
        login(randomEmail(), "가".repeat(25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void 정확히_72바이트인_비밀번호로도_가입하고_로그인할_수_있다() throws Exception {
        String password = "가".repeat(24);
        String email = signUpNewMember(password);

        login(email, password).andExpect(status().isOk());
    }

    @Test
    void 로그인으로_받은_액세스_토큰으로_보호된_API를_호출할_수_있다() throws Exception {
        String email = signUpNewMember(PASSWORD);
        String body = responseBody(login(email, PASSWORD).andExpect(status().isOk()));
        String accessToken = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/rooms").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void 로그인으로_받은_리프레시_토큰으로_토큰을_갱신할_수_있다() throws Exception {
        String email = signUpNewMember(PASSWORD);
        String body = responseBody(login(email, PASSWORD).andExpect(status().isOk()));
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    private String signUpNewMember(String password) throws Exception {
        String email = randomEmail();
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isCreated());
        return email;
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String responseBody(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String randomEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}

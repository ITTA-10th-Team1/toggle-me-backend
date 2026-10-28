package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
class LocalSignUpIntegrationTest {

    private static final String SIGN_UP_URL = "/api/v1/auth/signup";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalAccountRepository localAccountRepository;

    @Test
    void 일반_아이디로_가입하면_201과_토큰을_반환하고_비밀번호는_해시로_저장한다() throws Exception {
        String loginId = randomLoginId();
        String email = randomEmail();

        signUp(loginId, email, PASSWORD, "새닉네임")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.onboardingRequired").value(true));

        LocalAccount account = localAccountRepository.findWithMemberByLoginId(loginId).orElseThrow();
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(account.getMember().getNickname()).isEqualTo("새닉네임");
        assertThat(account.getMember().getHandle()).startsWith("user_");
    }

    @Test
    void 닉네임을_보내지_않으면_기본_닉네임으로_가입한다() throws Exception {
        String loginId = randomLoginId();

        signUp(loginId, randomEmail(), PASSWORD, null).andExpect(status().isCreated());

        assertThat(localAccountRepository.findWithMemberByLoginId(loginId).orElseThrow().getMember().getNickname())
                .isEqualTo("토글러");
    }

    @Test
    void 아이디와_이메일은_소문자로_정규화되어_저장된다() throws Exception {
        String loginId = randomLoginId();
        String email = randomEmail();

        signUp(loginId.toUpperCase(), email.toUpperCase(), PASSWORD, null).andExpect(status().isCreated());

        LocalAccount account = localAccountRepository.findWithMemberByLoginId(loginId).orElseThrow();
        assertThat(account.getLoginId()).isEqualTo(loginId);
        assertThat(account.getEmail()).isEqualTo(email);
    }

    @Test
    void 같은_아이디로_다시_가입하면_409를_반환한다() throws Exception {
        String loginId = randomLoginId();
        signUp(loginId, randomEmail(), PASSWORD, null).andExpect(status().isCreated());

        signUp(loginId, randomEmail(), PASSWORD, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
    }

    @Test
    void 대소문자만_다른_아이디로_다시_가입하면_409를_반환한다() throws Exception {
        String loginId = randomLoginId();
        signUp(loginId, randomEmail(), PASSWORD, null).andExpect(status().isCreated());

        signUp(loginId.toUpperCase(), randomEmail(), PASSWORD, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
    }

    @Test
    void 같은_이메일로_다른_아이디가_가입하면_이메일_중복_409를_반환한다() throws Exception {
        String email = randomEmail();
        signUp(randomLoginId(), email, PASSWORD, null).andExpect(status().isCreated());

        signUp(randomLoginId(), email, PASSWORD, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void 아이디와_이메일이_모두_겹치면_아이디_중복으로_응답한다() throws Exception {
        String loginId = randomLoginId();
        String email = randomEmail();
        signUp(loginId, email, PASSWORD, null).andExpect(status().isCreated());

        signUp(loginId, email, PASSWORD, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
    }

    @Test
    void 이메일_형식의_아이디는_가입할_수_없다() throws Exception {
        signUp("user@example.com", randomEmail(), PASSWORD, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("loginId"));
    }

    @Test
    void 아이디_앞뒤에_공백이_있으면_가입할_수_없다() throws Exception {
        signUp("  " + randomLoginId() + "  ", randomEmail(), PASSWORD, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("loginId"));
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        signUp(randomLoginId(), "not-an-email", PASSWORD, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void 필수값이_모두_없으면_아이디_이메일_비밀번호_오류를_함께_반환한다() throws Exception {
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("loginId", "email", "password")));
    }

    @Test
    void 이메일을_보내지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(randomLoginId(), PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void 비밀번호가_8자_미만이면_400을_반환한다() throws Exception {
        signUp(randomLoginId(), randomEmail(), "short12", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void 비밀번호가_72바이트를_넘으면_500이_아니라_400을_반환한다() throws Exception {
        signUp(randomLoginId(), randomEmail(), "가".repeat(25), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void 닉네임이_31자이면_400을_반환한다() throws Exception {
        signUp(randomLoginId(), randomEmail(), PASSWORD, "가".repeat(31))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
    }

    @Test
    void 요청_본문이_깨진_JSON이면_400을_반환한다() throws Exception {
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 성공_응답에는_비밀번호와_해시가_포함되지_않는다() throws Exception {
        signUp(randomLoginId(), randomEmail(), PASSWORD, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void 검증_오류_응답에는_입력한_비밀번호가_다시_나타나지_않는다() throws Exception {
        signUp("not valid!", randomEmail(), "secret-password-1", null)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("secret-password-1"))));
    }

    private ResultActions signUp(String loginId, String email, String password, String nickname) throws Exception {
        String nicknameField = nickname == null ? "" : ",\"nickname\":\"%s\"".formatted(nickname);
        return mockMvc.perform(post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"%s}"
                        .formatted(loginId, email, password, nicknameField)));
    }

    private String randomLoginId() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String randomEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}

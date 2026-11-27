package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.VerifiedEmailTokens;
import com.itta.toggleme.auth.client.EmailSender;
import com.itta.toggleme.auth.repository.EmailVerificationRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 비밀번호 재설정을 실제 DB로 확인한다. 틀린 횟수가 예외 뒤에도 남는지 보려고 테스트 트랜잭션을 쓰지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {

    private static final String CODES_URL = "/api/v1/auth/password-reset/codes";
    private static final String RESET_URL = "/api/v1/auth/password-reset";
    private static final String OLD_PASSWORD = "old-password1";
    private static final String NEW_PASSWORD = "new-password1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    private final List<String> createdEmails = new ArrayList<>();

    private record Account(String loginId, String email, String refreshToken) {
    }

    @AfterEach
    void cleanUp() {
        for (String email : createdEmails) {
            List<Long> memberIds = jdbcTemplate.queryForList(
                    "select member_id from local_account where email = ?", Long.class, email);
            for (Long memberId : memberIds) {
                jdbcTemplate.update("delete from refresh_token where member_id = ?", memberId);
                jdbcTemplate.update("delete from local_account where member_id = ?", memberId);
                jdbcTemplate.update("delete from member where id = ?", memberId);
            }
            jdbcTemplate.update("delete from email_verification where email = ?", email);
        }
        createdEmails.clear();
    }

    @Test
    void 가입된_이메일이면_204이고_재설정_코드_메일을_보낸다() throws Exception {
        Account account = signUpAccount();

        requestCode(account.email()).andExpect(status().isNoContent());

        assertThat(sentResetCode(account.email())).matches("\\d{6}");
        verify(emailSender, never()).sendVerificationCode(eq(account.email()), anyString());
    }

    @Test
    void 가입되지_않은_이메일도_204이고_메일은_보내지_않는다() throws Exception {
        String email = newEmail();

        requestCode(email).andExpect(status().isNoContent());

        verify(emailSender, never()).sendPasswordResetCode(eq(email), anyString());
    }

    @Test
    void 탈퇴한_회원의_이메일은_메일을_보내지_않는다() throws Exception {
        Account account = signUpAccount();
        jdbcTemplate.update("update member set status = 'WITHDRAWN' where id = "
                + "(select member_id from local_account where email = ?)", account.email());

        requestCode(account.email()).andExpect(status().isNoContent());

        verify(emailSender, never()).sendPasswordResetCode(eq(account.email()), anyString());
    }

    @Test
    void 같은_이메일로_60초_안에_다시_요청하면_429를_반환한다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());

        requestCode(account.email())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOO_MANY_REQUESTS"));
    }

    @Test
    void 코드가_맞으면_비밀번호가_바뀌어_새_비밀번호로만_로그인할_수_있다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());

        reset(account.email(), sentResetCode(account.email()), NEW_PASSWORD).andExpect(status().isNoContent());

        login(account.loginId(), NEW_PASSWORD).andExpect(status().isOk());
        login(account.loginId(), OLD_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENTIALS_INVALID"));
    }

    @Test
    void 비밀번호를_바꾸면_기존_기기의_refresh_token이_모두_폐기된다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());

        reset(account.email(), sentResetCode(account.email()), NEW_PASSWORD).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 코드를_확인한_계정은_이메일_인증_시각이_남는다() throws Exception {
        Account account = signUpAccount();
        jdbcTemplate.update("update local_account set email_verified_at = null where email = ?", account.email());
        requestCode(account.email()).andExpect(status().isNoContent());

        reset(account.email(), sentResetCode(account.email()), NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from local_account where email = ? and email_verified_at is not null",
                Long.class, account.email())).isEqualTo(1);
    }

    @Test
    void 코드가_틀리면_400이고_비밀번호는_그대로이며_틀린_횟수가_저장된다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());

        reset(account.email(), wrongCode(sentResetCode(account.email())), NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));

        login(account.loginId(), OLD_PASSWORD).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "select failed_attempts from email_verification where email = ? and purpose = 'PASSWORD_RESET'",
                Long.class, account.email())).isEqualTo(1);
    }

    @Test
    void 다섯_번_틀리면_맞는_코드도_거절한다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());
        String code = sentResetCode(account.email());
        for (int i = 0; i < 5; i++) {
            reset(account.email(), wrongCode(code), NEW_PASSWORD).andExpect(status().isBadRequest());
        }

        reset(account.email(), code, NEW_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS"));
        login(account.loginId(), OLD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 만료된_코드는_400을_반환한다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());
        String code = sentResetCode(account.email());
        jdbcTemplate.update("update email_verification set expires_at = date_sub(now(6), interval 1 minute)"
                + " where email = ? and purpose = 'PASSWORD_RESET'", account.email());

        reset(account.email(), code, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    void 한_번_쓴_코드로는_다시_바꿀_수_없다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());
        String code = sentResetCode(account.email());
        reset(account.email(), code, NEW_PASSWORD).andExpect(status().isNoContent());

        reset(account.email(), code, "another-password1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));
        login(account.loginId(), NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 요청한_적_없는_이메일은_코드가_틀린_것과_같은_응답을_준다() throws Exception {
        reset(newEmail(), "123456", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));
    }

    @Test
    void 가입_인증_코드가_맞아도_재설정_코드가_없으면_비밀번호를_바꿀_수_없다() throws Exception {
        Account account = signUpAccount();
        // 가입 인증용 행의 코드를 아는 값(123456)으로 맞춰 둔다. 재설정 코드는 요청한 적이 없다
        jdbcTemplate.update("update email_verification set code_hash = ? where email = ? and purpose = 'SIGNUP'",
                sha256(account.email() + ":123456"), account.email());

        reset(account.email(), "123456", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));

        login(account.loginId(), OLD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 새_비밀번호가_8자_미만이면_400이고_코드는_소모되지_않는다() throws Exception {
        Account account = signUpAccount();
        requestCode(account.email()).andExpect(status().isNoContent());
        String code = sentResetCode(account.email());

        reset(account.email(), code, "short12")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));

        reset(account.email(), code, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void 코드_형식이_숫자_6자리가_아니면_400을_반환한다() throws Exception {
        reset(newEmail(), "12ab56", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("code"));
    }

    private Account signUpAccount() throws Exception {
        String email = newEmail();
        String loginId = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String token = VerifiedEmailTokens.issue(emailVerificationRepository, email);
        String body = mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"loginId\":\"%s\",\"email\":\"%s\",\"emailVerificationToken\":\"%s\","
                                + "\"password\":\"%s\"}").formatted(loginId, email, token, OLD_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(loginId, email, JsonPath.read(body, "$.refreshToken"));
    }

    private ResultActions requestCode(String email) throws Exception {
        return mockMvc.perform(post(CODES_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions reset(String email, String code, String newPassword) throws Exception {
        return mockMvc.perform(post(RESET_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}"
                        .formatted(email, code, newPassword)));
    }

    private ResultActions login(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"%s\",\"password\":\"%s\"}".formatted(loginId, password)));
    }

    private String sentResetCode(String email) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).sendPasswordResetCode(eq(email), code.capture());
        return code.getValue();
    }

    private String sha256(String value) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    private String newEmail() {
        String email = "pr-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
        createdEmails.add(email);
        return email;
    }
}

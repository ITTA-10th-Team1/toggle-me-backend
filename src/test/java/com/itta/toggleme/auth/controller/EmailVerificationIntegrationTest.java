package com.itta.toggleme.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.auth.client.EmailSender;
import com.itta.toggleme.auth.exception.EmailSendFailedException;
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

/** 이메일 인증 흐름을 실제 DB로 확인한다. 틀린 횟수가 예외 뒤에도 남는지 보려고 테스트 트랜잭션을 쓰지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationIntegrationTest {

    private static final String SEND_URL = "/api/v1/auth/email-verifications";
    private static final String CONFIRM_URL = "/api/v1/auth/email-verifications/confirm";
    private static final String SIGN_UP_URL = "/api/v1/auth/signup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSender emailSender;

    private final List<String> createdEmails = new ArrayList<>();

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
    void 코드를_요청하면_204이고_메일로_6자리_코드를_보내며_코드_원문은_저장하지_않는다() throws Exception {
        String email = newEmail();

        send(email).andExpect(status().isNoContent());

        String code = sentCode(email);
        assertThat(code).matches("\\d{6}");
        String storedHash = jdbcTemplate.queryForObject(
                "select code_hash from email_verification where email = ?", String.class, email);
        assertThat(storedHash).isNotEqualTo(code).hasSize(64);
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        send("not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 이미_가입된_이메일이면_409를_반환하고_메일을_보내지_않는다() throws Exception {
        String email = newEmail();
        signUpWithVerification(email, newLoginId()).andExpect(status().isCreated());

        send(email)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void 코드를_요청하고_60초_안에_다시_요청하면_429를_반환한다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());

        send(email)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOO_MANY_REQUESTS"));
    }

    @Test
    void 한_시간에_다섯_번을_넘겨_요청하면_429를_반환한다() throws Exception {
        String email = newEmail();
        for (int i = 0; i < 5; i++) {
            jdbcTemplate.update("""
                    insert into email_verification (email, code_hash, expires_at, failed_attempts, created_at, updated_at)
                    values (?, 'x', now(6), 0, date_sub(now(6), interval 5 minute), now(6))
                    """, email);
        }

        send(email)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOO_MANY_REQUESTS"));
    }

    @Test
    void 메일_발송에_실패하면_502이고_요청_기록이_남지_않아_바로_다시_요청할_수_있다() throws Exception {
        String email = newEmail();
        willThrow(new EmailSendFailedException()).given(emailSender).sendVerificationCode(eq(email), anyString());

        send(email)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EMAIL_SEND_FAILED"));

        assertThat(count("select count(*) from email_verification where email = ?", email)).isZero();
    }

    @Test
    void 올바른_코드를_입력하면_토큰을_받아_가입할_수_있고_이메일_인증_시각이_저장된다() throws Exception {
        String email = newEmail();
        String loginId = newLoginId();
        send(email).andExpect(status().isNoContent());

        String token = confirmAndGetToken(email, sentCode(email));
        signUp(email, loginId, token).andExpect(status().isCreated());

        assertThat(count("select count(*) from local_account where email = ? and email_verified_at is not null",
                email)).isEqualTo(1);
    }

    @Test
    void 코드를_틀리면_400이고_틀린_횟수가_저장된다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());

        confirm(email, wrongCode(sentCode(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));

        assertThat(count("select failed_attempts from email_verification where email = ?", email)).isEqualTo(1);
    }

    @Test
    void 다섯_번_틀리면_맞는_코드도_거절하고_코드를_다시_요청해야_한다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());
        String code = sentCode(email);
        for (int i = 0; i < 5; i++) {
            confirm(email, wrongCode(code)).andExpect(status().isBadRequest());
        }

        confirm(email, code)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS"));
    }

    @Test
    void 만료된_코드는_400을_반환한다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());
        String code = sentCode(email);
        jdbcTemplate.update("update email_verification set expires_at = date_sub(now(6), interval 1 minute)"
                + " where email = ?", email);

        confirm(email, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    void 요청한_적_없는_이메일을_확인하면_코드_오류를_반환한다() throws Exception {
        confirm(newEmail(), "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));
    }

    @Test
    void 이미_확인을_마친_코드로_다시_확인하면_거절한다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());
        String code = sentCode(email);
        confirmAndGetToken(email, code);

        confirm(email, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_INVALID"));
    }

    @Test
    void 코드_형식이_숫자_6자리가_아니면_400을_반환한다() throws Exception {
        confirm(newEmail(), "12ab56")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 토큰이_만료되면_가입할_수_없다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());
        String token = confirmAndGetToken(email, sentCode(email));
        jdbcTemplate.update("update email_verification set token_expires_at = date_sub(now(6), interval 1 minute)"
                + " where email = ?", email);

        signUp(email, newLoginId(), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));
    }

    @Test
    void 가입이_다른_이유로_실패하면_토큰을_다시_쓸_수_있다() throws Exception {
        String email = newEmail();
        String takenLoginId = newLoginId();
        signUpWithVerification(newEmail(), takenLoginId).andExpect(status().isCreated());
        send(email).andExpect(status().isNoContent());
        String token = confirmAndGetToken(email, sentCode(email));

        signUp(email, takenLoginId, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
        signUp(email, newLoginId(), token).andExpect(status().isCreated());
    }

    @Test
    void 응답에_코드나_해시가_포함되지_않는다() throws Exception {
        String email = newEmail();
        send(email).andExpect(status().isNoContent());
        String code = sentCode(email);

        String body = confirm(email, code).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(body, "$.verificationToken")).isNotBlank();
        assertThat(body).doesNotContain(code).doesNotContain("hash");
    }

    private ResultActions send(String email) throws Exception {
        return mockMvc.perform(post(SEND_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions confirm(String email, String code) throws Exception {
        return mockMvc.perform(post(CONFIRM_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code)));
    }

    private String confirmAndGetToken(String email, String code) throws Exception {
        String body = confirm(email, code).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.verificationToken");
    }

    private ResultActions signUp(String email, String loginId, String token) throws Exception {
        return mockMvc.perform(post(SIGN_UP_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"%s\",\"email\":\"%s\",\"emailVerificationToken\":\"%s\","
                        .formatted(loginId, email, token) + "\"password\":\"password123\"}"));
    }

    private ResultActions signUpWithVerification(String email, String loginId) throws Exception {
        send(email).andExpect(status().isNoContent());
        return signUp(email, loginId, confirmAndGetToken(email, sentCode(email)));
    }

    /** 마지막으로 보낸 메일의 코드. 같은 이메일로 여러 번 보냈으면 가장 최근 것이다. */
    private String sentCode(String email) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailSender, org.mockito.Mockito.atLeastOnce()).sendVerificationCode(eq(email), code.capture());
        return code.getValue();
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    private long count(String sql, String email) {
        return jdbcTemplate.queryForObject(sql, Long.class, email);
    }

    private String newEmail() {
        String email = "ev-" + UUID.randomUUID().toString().substring(0, 12) + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private String newLoginId() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

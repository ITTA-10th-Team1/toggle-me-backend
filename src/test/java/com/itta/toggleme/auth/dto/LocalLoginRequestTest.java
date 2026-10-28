package com.itta.toggleme.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LocalLoginRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String VALID_LOGIN_ID = "chaeee22";
    private static final String VALID_PASSWORD = "password123";

    @Test
    void 올바른_요청은_검증을_통과한다() {
        assertThat(violatedFields(VALID_LOGIN_ID, VALID_PASSWORD)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void 아이디가_비어_있으면_검증에_실패한다(String loginId) {
        assertThat(violatedFields(loginId, VALID_PASSWORD)).containsExactly("loginId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void 비밀번호가_비어_있으면_검증에_실패한다(String password) {
        assertThat(violatedFields(VALID_LOGIN_ID, password)).containsExactly("password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "not valid id!", "a", "채이이이이"})
    void 아이디_형식은_검증하지_않아서_규칙이_바뀌어도_기존_계정이_로그인된다(String loginId) {
        assertThat(violatedFields(loginId, VALID_PASSWORD)).isEmpty();
    }

    @Test
    void 비밀번호_최소_길이는_검증하지_않는다() {
        assertThat(violatedFields(VALID_LOGIN_ID, "a")).isEmpty();
    }

    @Test
    void 비밀번호가_72바이트를_넘으면_검증에_실패한다() {
        assertThat(violatedFields(VALID_LOGIN_ID, "a".repeat(72))).isEmpty();
        assertThat(violatedFields(VALID_LOGIN_ID, "a".repeat(73))).containsExactly("password");
        assertThat(violatedFields(VALID_LOGIN_ID, "가".repeat(24))).isEmpty();
        assertThat(violatedFields(VALID_LOGIN_ID, "가".repeat(25))).containsExactly("password");
    }

    @Test
    void 아이디가_255자를_넘으면_검증에_실패한다() {
        assertThat(violatedFields("a".repeat(255), VALID_PASSWORD)).isEmpty();
        assertThat(violatedFields("a".repeat(256), VALID_PASSWORD)).containsExactly("loginId");
    }

    @Test
    void toString에_비밀번호가_포함되지_않는다() {
        LocalLoginRequest request = new LocalLoginRequest(VALID_LOGIN_ID, "secret-password-1");

        assertThat(request.toString())
                .contains(VALID_LOGIN_ID)
                .doesNotContain("secret-password-1");
    }

    private Set<String> violatedFields(String loginId, String password) {
        return VALIDATOR.validate(new LocalLoginRequest(loginId, password)).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}

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
    private static final String VALID_EMAIL = "user@example.com";
    private static final String VALID_PASSWORD = "password123";

    @Test
    void 올바른_요청은_검증을_통과한다() {
        assertThat(violatedFields(VALID_EMAIL, VALID_PASSWORD)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void 이메일이_비어_있으면_검증에_실패한다(String email) {
        assertThat(violatedFields(email, VALID_PASSWORD)).containsExactly("loginId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void 비밀번호가_비어_있으면_검증에_실패한다(String password) {
        assertThat(violatedFields(VALID_EMAIL, password)).containsExactly("password");
    }

    @Test
    void 이메일_형식은_검증하지_않고_인증_실패로_처리하도록_통과시킨다() {
        assertThat(violatedFields("not-an-email", VALID_PASSWORD)).isEmpty();
    }

    @Test
    void 비밀번호_최소_길이는_검증하지_않는다() {
        assertThat(violatedFields(VALID_EMAIL, "a")).isEmpty();
    }

    @Test
    void 비밀번호가_72바이트를_넘으면_검증에_실패한다() {
        assertThat(violatedFields(VALID_EMAIL, "a".repeat(72))).isEmpty();
        assertThat(violatedFields(VALID_EMAIL, "a".repeat(73))).containsExactly("password");
        assertThat(violatedFields(VALID_EMAIL, "가".repeat(24))).isEmpty();
        assertThat(violatedFields(VALID_EMAIL, "가".repeat(25))).containsExactly("password");
    }

    @Test
    void 이메일이_255자를_넘으면_검증에_실패한다() {
        assertThat(violatedFields("a".repeat(256), VALID_PASSWORD)).containsExactly("loginId");
    }

    @Test
    void toString에_비밀번호가_포함되지_않는다() {
        LocalLoginRequest request = new LocalLoginRequest(VALID_EMAIL, "secret-password-1");

        assertThat(request.toString())
                .contains(VALID_EMAIL)
                .doesNotContain("secret-password-1");
    }

    private Set<String> violatedFields(String email, String password) {
        return VALIDATOR.validate(new LocalLoginRequest(email, password)).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}

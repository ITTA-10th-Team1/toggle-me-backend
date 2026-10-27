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

class LocalSignUpRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String VALID_EMAIL = "user@example.com";
    private static final String VALID_PASSWORD = "password123";

    @Test
    void 올바른_요청은_검증을_통과한다() {
        assertThat(violatedFields(VALID_EMAIL, VALID_PASSWORD, "토글러")).isEmpty();
    }

    @Test
    void 닉네임은_생략할_수_있다() {
        assertThat(violatedFields(VALID_EMAIL, VALID_PASSWORD, null)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void 이메일이_비어_있으면_검증에_실패한다(String email) {
        assertThat(violatedFields(email, VALID_PASSWORD, null)).containsExactly("loginId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain", "user@", "@example.com", "user name@example.com"})
    void 이메일_형식이_잘못되면_검증에_실패한다(String email) {
        assertThat(violatedFields(email, VALID_PASSWORD, null)).containsExactly("loginId");
    }

    @Test
    void 이메일이_255자를_넘으면_검증에_실패한다() {
        String longEmail = "a".repeat(250) + "@example.com";

        assertThat(violatedFields(longEmail, VALID_PASSWORD, null)).containsExactly("loginId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "        "})
    void 비밀번호가_비어_있으면_검증에_실패한다(String password) {
        assertThat(violatedFields(VALID_EMAIL, password, null)).containsExactly("password");
    }

    @Test
    void 비밀번호는_8자_미만이면_실패하고_8자면_통과한다() {
        assertThat(violatedFields(VALID_EMAIL, "1234567", null)).containsExactly("password");
        assertThat(violatedFields(VALID_EMAIL, "12345678", null)).isEmpty();
    }

    @Test
    void 영문_72자_비밀번호는_통과하고_73자는_실패한다() {
        assertThat(violatedFields(VALID_EMAIL, "a".repeat(72), null)).isEmpty();
        assertThat(violatedFields(VALID_EMAIL, "a".repeat(73), null)).containsExactly("password");
    }

    @Test
    void 한글_24자_72바이트_비밀번호는_통과하고_25자는_실패한다() {
        assertThat(violatedFields(VALID_EMAIL, "가".repeat(24), null)).isEmpty();
        assertThat(violatedFields(VALID_EMAIL, "가".repeat(25), null)).containsExactly("password");
    }

    @Test
    void 영문과_한글이_섞여_73바이트가_되면_실패한다() {
        assertThat(violatedFields(VALID_EMAIL, "a" + "가".repeat(24), null)).containsExactly("password");
    }

    @Test
    void 닉네임은_30자까지_허용하고_31자는_실패한다() {
        assertThat(violatedFields(VALID_EMAIL, VALID_PASSWORD, "가".repeat(30))).isEmpty();
        assertThat(violatedFields(VALID_EMAIL, VALID_PASSWORD, "가".repeat(31))).containsExactly("nickname");
    }

    @Test
    void 여러_필드가_잘못되면_모두_알려준다() {
        assertThat(violatedFields("invalid", "short", "가".repeat(31)))
                .containsExactlyInAnyOrder("loginId", "password", "nickname");
    }

    @Test
    void toString에_비밀번호가_포함되지_않는다() {
        LocalSignUpRequest request = new LocalSignUpRequest(VALID_EMAIL, "secret-password-1", "토글러");

        assertThat(request.toString())
                .contains(VALID_EMAIL)
                .doesNotContain("secret-password-1");
    }

    private Set<String> violatedFields(String email, String password, String nickname) {
        return VALIDATOR.validate(new LocalSignUpRequest(email, password, nickname)).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}

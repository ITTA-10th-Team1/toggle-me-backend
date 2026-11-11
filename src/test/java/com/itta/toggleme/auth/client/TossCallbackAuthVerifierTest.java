package com.itta.toggleme.auth.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.auth.exception.TossCallbackUnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TossCallbackAuthVerifierTest {

    @Test
    void 아이디와_비밀번호가_설정되어_있고_일치하면_통과한다() {
        TossCallbackAuthVerifier verifier = verifier("user", "pass");

        assertThatCode(() -> verifier.verify(basic("user:pass"))).doesNotThrowAnyException();
    }

    @Test
    void 비밀번호에_콜론이_들어_있어도_통과한다() {
        TossCallbackAuthVerifier verifier = verifier("user", "pa:ss");

        assertThatCode(() -> verifier.verify(basic("user:pa:ss"))).doesNotThrowAnyException();
    }

    @Test
    void 설정이_비어_있으면_빈_인증도_거절한다() {
        TossCallbackAuthVerifier notConfigured = verifier("", "");

        assertThatThrownBy(() -> notConfigured.verify(basic(":"))).isInstanceOf(TossCallbackUnauthorizedException.class);
        assertThatThrownBy(() -> notConfigured.verify(null)).isInstanceOf(TossCallbackUnauthorizedException.class);
    }

    @Test
    void 헤더가_없거나_Basic이_아니면_거절한다() {
        TossCallbackAuthVerifier verifier = verifier("user", "pass");

        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(TossCallbackUnauthorizedException.class);
        assertThatThrownBy(() -> verifier.verify("Bearer abc")).isInstanceOf(TossCallbackUnauthorizedException.class);
    }

    private TossCallbackAuthVerifier verifier(String username, String password) {
        return new TossCallbackAuthVerifier(new TossProperties("https://toss.test", "", "", username, password));
    }

    private String basic(String credentials) {
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}

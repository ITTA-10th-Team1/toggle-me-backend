package com.itta.toggleme.auth;

import com.itta.toggleme.auth.domain.EmailVerification;
import com.itta.toggleme.auth.repository.EmailVerificationRepository;
import com.itta.toggleme.member.domain.LocalAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/** 테스트에서 이메일 인증을 마친 상태(가입에 쓸 verificationToken)를 바로 만든다. */
public final class VerifiedEmailTokens {

    private VerifiedEmailTokens() {
    }

    public static String issue(EmailVerificationRepository repository, String email) {
        String normalizedEmail = LocalAccount.normalizeEmail(email);
        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        EmailVerification verification = EmailVerification.issue(normalizedEmail, sha256("unused"), now.plusSeconds(600));
        verification.markVerified(sha256(token), now.plusSeconds(1800), now);
        repository.saveAndFlush(verification);
        return token;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

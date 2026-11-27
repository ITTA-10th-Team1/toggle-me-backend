package com.itta.toggleme.auth.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가입 전 이메일 소유 확인. 코드를 맞히면 1회용 토큰(verificationToken)이 발급되고, 가입이 그 토큰을 소비한다.
 * 코드와 토큰은 해시만 저장한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailVerificationPurpose purpose;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int failedAttempts;

    private Instant verifiedAt;

    @Column(length = 64)
    private String tokenHash;

    private Instant tokenExpiresAt;

    private Instant consumedAt;

    private EmailVerification(String email, EmailVerificationPurpose purpose, String codeHash, Instant expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification issue(String email, EmailVerificationPurpose purpose, String codeHash,
                                          Instant expiresAt) {
        return new EmailVerification(email, purpose, codeHash, expiresAt);
    }

    public boolean isCodeExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void recordFailure() {
        failedAttempts++;
    }

    /** 코드를 맞혔다고 표시한다. 토큰이 필요 없는 흐름(비밀번호 재설정)에서 쓴다. */
    public void markVerified(Instant now) {
        this.verifiedAt = now;
    }

    public void markVerified(String tokenHash, Instant tokenExpiresAt, Instant now) {
        this.verifiedAt = now;
        this.tokenHash = tokenHash;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean isTokenUsable(Instant now) {
        return verifiedAt != null && consumedAt == null && tokenExpiresAt != null && tokenExpiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }
}

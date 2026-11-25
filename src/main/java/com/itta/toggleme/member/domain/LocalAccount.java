package com.itta.toggleme.member.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 255)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String email;

    private Instant emailVerifiedAt;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    private LocalAccount(Member member, String loginId, String email, Instant emailVerifiedAt,
                         String passwordHash) {
        this.member = member;
        this.loginId = loginId;
        this.email = email;
        this.emailVerifiedAt = emailVerifiedAt;
        this.passwordHash = passwordHash;
    }

    /** 이메일 인증(emailVerifiedAt)을 마친 가입이면 인증 시각을 넘긴다. 인증 도입 전 계정은 null이다. */
    public static LocalAccount create(Member member, String loginId, String email, Instant emailVerifiedAt,
                                      String passwordHash) {
        return new LocalAccount(member, normalizeLoginId(loginId), normalizeEmail(email), emailVerifiedAt,
                passwordHash);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** 이메일 인증을 아직 안 한 계정이 코드 확인을 통과하면 인증 시각을 남긴다. */
    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    public static String normalizeLoginId(String loginId) {
        return normalize(loginId);
    }

    public static String normalizeEmail(String email) {
        return normalize(email);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}

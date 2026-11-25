package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.client.EmailSender;
import com.itta.toggleme.auth.domain.EmailVerification;
import com.itta.toggleme.auth.exception.EmailAlreadyExistsException;
import com.itta.toggleme.auth.exception.EmailVerificationCodeExpiredException;
import com.itta.toggleme.auth.exception.EmailVerificationCodeInvalidException;
import com.itta.toggleme.auth.exception.EmailVerificationRequiredException;
import com.itta.toggleme.auth.exception.EmailVerificationTooManyAttemptsException;
import com.itta.toggleme.auth.exception.EmailVerificationTooManyRequestsException;
import com.itta.toggleme.auth.repository.EmailVerificationRepository;
import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입 전 이메일 소유 확인. 코드 발송 → 코드 확인(토큰 발급) → 가입 때 토큰 소비 순서다.
 * 코드는 6자리 숫자라 무차별 대입을 막기 위해 재요청 간격, 시간당 발송 수, 틀린 횟수를 제한한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    static final Duration CODE_TTL = Duration.ofMinutes(10);
    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
    static final Duration HOURLY_WINDOW = Duration.ofHours(1);
    static final int MAX_REQUESTS_PER_HOUR = 5;
    static final int MAX_FAILED_ATTEMPTS = 5;

    private final EmailVerificationRepository emailVerificationRepository;
    private final LocalAccountRepository localAccountRepository;
    private final EmailSender emailSender;
    private final SecureRandom random = new SecureRandom();

    /** 코드를 저장하고 메일을 보낸다. 메일 발송이 실패하면 저장도 함께 취소돼 재요청 대기 시간이 생기지 않는다. */
    @Transactional
    public void requestCode(String rawEmail) {
        String email = LocalAccount.normalizeEmail(rawEmail);
        if (localAccountRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        Instant now = Instant.now();
        emailVerificationRepository.findFirstByEmailOrderByIdDesc(email)
                .filter(latest -> latest.getCreatedAt().isAfter(now.minus(RESEND_INTERVAL)))
                .ifPresent(latest -> {
                    throw new EmailVerificationTooManyRequestsException();
                });
        if (emailVerificationRepository.countByEmailAndCreatedAtAfter(email, now.minus(HOURLY_WINDOW))
                >= MAX_REQUESTS_PER_HOUR) {
            throw new EmailVerificationTooManyRequestsException();
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        emailVerificationRepository.saveAndFlush(
                EmailVerification.issue(email, hashCode(email, code), now.plus(CODE_TTL)));
        emailSender.sendVerificationCode(email, code);
    }

    /** 코드가 맞으면 가입에 쓸 1회용 토큰을 돌려준다. 틀린 횟수는 예외가 나도 남겨야 해서 롤백하지 않는다. */
    @Transactional(noRollbackFor = BusinessException.class)
    public String confirmCode(String rawEmail, String code) {
        String email = LocalAccount.normalizeEmail(rawEmail);
        Instant now = Instant.now();
        EmailVerification verification = emailVerificationRepository.findLatestByEmailForUpdate(email)
                .orElseThrow(EmailVerificationCodeInvalidException::new);

        if (verification.isVerified()) {
            throw new EmailVerificationCodeInvalidException();
        }
        if (verification.isCodeExpired(now)) {
            throw new EmailVerificationCodeExpiredException();
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new EmailVerificationTooManyAttemptsException();
        }
        if (!MessageDigest.isEqual(hashCode(email, code).getBytes(StandardCharsets.UTF_8),
                verification.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            verification.recordFailure();
            throw new EmailVerificationCodeInvalidException();
        }

        String token = newToken();
        verification.markVerified(TokenService.hash(token), now.plus(TOKEN_TTL), now);
        return token;
    }

    /** 가입 트랜잭션 안에서 호출한다. 가입이 실패해 롤백되면 토큰 소비도 함께 취소된다. */
    @Transactional
    public Instant consumeToken(String rawEmail, String token) {
        String email = LocalAccount.normalizeEmail(rawEmail);
        Instant now = Instant.now();
        EmailVerification verification = emailVerificationRepository
                .findByTokenHashForUpdate(TokenService.hash(token))
                .filter(found -> found.getEmail().equals(email))
                .filter(found -> found.isTokenUsable(now))
                .orElseThrow(EmailVerificationRequiredException::new);
        verification.consume(now);
        return verification.getVerifiedAt();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashCode(String email, String code) {
        return TokenService.hash(email + ":" + code);
    }
}

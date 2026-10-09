package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.domain.RefreshToken;
import com.itta.toggleme.auth.dto.TokenResponse;
import com.itta.toggleme.auth.exception.InvalidRefreshTokenException;
import com.itta.toggleme.auth.repository.RefreshTokenRepository;
import com.itta.toggleme.global.security.JwtProperties;
import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.member.domain.Member;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public TokenResponse issue(Member member) {
        return issue(member, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenResponse reissue(String refreshToken) {
        Instant now = Instant.now();
        RefreshToken savedToken = refreshTokenRepository.findByTokenHashForUpdate(hash(refreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (savedToken.isRevoked()) {
            refreshTokenRepository.revokeAllActiveInSession(savedToken.getSessionId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (savedToken.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        savedToken.revoke(now);
        return issue(savedToken.getMember(), savedToken.getSessionId());
    }

    @Transactional
    public void revoke(Long memberId, String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .filter(token -> token.getMember().getId().equals(memberId))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private TokenResponse issue(Member member, String sessionId) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = generateRefreshToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenExpiry());
        refreshTokenRepository.save(RefreshToken.issue(member, sessionId, hash(refreshToken), expiresAt));
        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirySeconds());
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}

package com.itta.toggleme.auth.repository;

import com.itta.toggleme.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revokedAt = :now, rt.updatedAt = :now
            where rt.sessionId = :sessionId and rt.revokedAt is null
            """)
    int revokeAllActiveInSession(String sessionId, Instant now);
}

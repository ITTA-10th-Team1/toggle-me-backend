package com.itta.toggleme.auth.repository;

import com.itta.toggleme.auth.domain.EmailVerification;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByEmailOrderByIdDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.id = "
            + "(select max(latest.id) from EmailVerification latest where latest.email = :email)")
    Optional<EmailVerification> findLatestByEmailForUpdate(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.tokenHash = :tokenHash")
    Optional<EmailVerification> findByTokenHashForUpdate(String tokenHash);

    long countByEmailAndCreatedAtAfter(String email, Instant since);
}

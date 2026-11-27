package com.itta.toggleme.auth.repository;

import com.itta.toggleme.auth.domain.EmailVerification;
import com.itta.toggleme.auth.domain.EmailVerificationPurpose;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByEmailAndPurposeOrderByIdDesc(String email, EmailVerificationPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.id = (select max(latest.id) from EmailVerification latest "
            + "where latest.email = :email and latest.purpose = :purpose)")
    Optional<EmailVerification> findLatestForUpdate(String email, EmailVerificationPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.tokenHash = :tokenHash")
    Optional<EmailVerification> findByTokenHashForUpdate(String tokenHash);

    long countByEmailAndPurposeAndCreatedAtAfter(String email, EmailVerificationPurpose purpose, Instant since);
}

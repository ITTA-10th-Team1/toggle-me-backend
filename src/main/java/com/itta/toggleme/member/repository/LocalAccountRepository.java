package com.itta.toggleme.member.repository;

import com.itta.toggleme.member.domain.LocalAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

    @Query("""
            select la from LocalAccount la
            join fetch la.member
            where la.loginId = :loginId
            """)
    Optional<LocalAccount> findWithMemberByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);
}

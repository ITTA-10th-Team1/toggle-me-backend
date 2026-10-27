package com.itta.toggleme.member.repository;

import com.itta.toggleme.member.domain.LocalAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

    @Query("""
            select la from LocalAccount la
            join fetch la.member
            where la.email = :email
            """)
    Optional<LocalAccount> findWithMemberByEmail(String email);

    boolean existsByEmail(String email);
}

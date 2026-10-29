package com.itta.toggleme.member.repository;

import com.itta.toggleme.member.domain.SocialAccount;
import com.itta.toggleme.member.domain.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    @Query("""
            select sa from SocialAccount sa
            join fetch sa.member
            where sa.provider = :provider and sa.providerUserId = :providerUserId
            """)
    Optional<SocialAccount> findWithMember(SocialProvider provider, String providerUserId);
}

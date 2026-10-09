package com.itta.toggleme.member.repository;

import com.itta.toggleme.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByHandle(String handle);
}

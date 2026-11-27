package com.itta.toggleme.member.repository;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.MemberStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // outer join·DISTINCT·집계를 넣으면 follow-on locking으로 바뀌어 잠금 보장이 깨진다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    // 이 컬럼만 바꾸는 조건부 UPDATE라 동시에 다른 회원 정보가 바뀌어도 서로 덮어쓰지 않는다. 바뀐 행 수를 돌려준다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Member m set m.tutorialCompleted = true where m.id = :id and m.onboardingCompleted = true")
    int completeTutorial(@Param("id") Long id);

    boolean existsByHandle(String handle);

    Optional<Member> findByHandleAndStatus(String handle, MemberStatus status);

    long countByStatus(MemberStatus status);

    @Query("select m.createdAt from Member m where m.createdAt >= :from")
    List<Instant> findCreatedAtSince(@Param("from") Instant from);

    long countByStatusAndCreatedAtGreaterThanEqual(MemberStatus status, Instant from);

    long countByStatusAndProfileCompletedTrueAndCreatedAtGreaterThanEqual(MemberStatus status, Instant from);
}

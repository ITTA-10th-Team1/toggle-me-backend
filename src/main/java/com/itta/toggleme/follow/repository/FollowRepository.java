package com.itta.toggleme.follow.repository;

import com.itta.toggleme.follow.domain.Follow;
import com.itta.toggleme.member.domain.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 목록·수에는 탈퇴하지 않았고 방이 ON인 회원만 센다(PRD ROOM-33). 팔로우 행은 남아 있어 다시 ON이면 돌아온다
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 이미 팔로우 중이거나 동시에 같은 요청이 와도 한 행만 남는다
    @Modifying
    @Query(value = """
            insert ignore into follow (follower_id, followee_id, created_at, updated_at)
            values (:followerId, :followeeId, utc_timestamp(6), utc_timestamp(6))
            """, nativeQuery = true)
    void insertIfAbsent(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    @Modifying
    @Query("delete from Follow f where f.follower.id = :followerId and f.followee.id = :followeeId")
    void deleteByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Query("""
            select count(f) from Follow f
            where f.followee.id = :memberId and f.follower.status = 'ACTIVE'
              and exists (select 1 from Room r where r.owner = f.follower
                          and r.status = 'ACTIVE' and r.visibility = 'SOCIAL_ON')
            """)
    long countFollowers(@Param("memberId") Long memberId);

    @Query("""
            select count(f) from Follow f
            where f.follower.id = :memberId and f.followee.status = 'ACTIVE'
              and exists (select 1 from Room r where r.owner = f.followee
                          and r.status = 'ACTIVE' and r.visibility = 'SOCIAL_ON')
            """)
    long countFollowing(@Param("memberId") Long memberId);

    // ponytail: 페이지 없이 전부 준다. 팔로워가 수백 명을 넘으면 커서 페이지로 바꾼다
    @Query("""
            select f.follower from Follow f
            where f.followee.id = :memberId and f.follower.status = 'ACTIVE'
              and exists (select 1 from Room r where r.owner = f.follower
                          and r.status = 'ACTIVE' and r.visibility = 'SOCIAL_ON')
            order by f.id desc
            """)
    List<Member> findFollowers(@Param("memberId") Long memberId);

    @Query("""
            select f.followee from Follow f
            where f.follower.id = :memberId and f.followee.status = 'ACTIVE'
              and exists (select 1 from Room r where r.owner = f.followee
                          and r.status = 'ACTIVE' and r.visibility = 'SOCIAL_ON')
            order by f.id desc
            """)
    List<Member> findFollowing(@Param("memberId") Long memberId);
}

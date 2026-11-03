package com.itta.toggleme.follow.repository;

import com.itta.toggleme.follow.dto.MemberSearchResponse;
import com.itta.toggleme.member.domain.Member;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MemberSearchRepository extends Repository<Member, Long> {

    // 고유 ID 앞부분 일치(uk 인덱스 사용). 탈퇴·방 OFF 회원은 빼고, 나는 OFF여도 나온다
    @Query("""
            select new com.itta.toggleme.follow.dto.MemberSearchResponse(
                m.id, m.handle, m.nickname,
                case when exists (select 1 from Follow f where f.follower.id = :viewerId and f.followee = m)
                     then true else false end)
            from Member m
            where m.handle like :prefix escape '!' and m.status = 'ACTIVE'
              and (m.id = :viewerId
                   or exists (select 1 from Room r where r.owner = m
                              and r.status = 'ACTIVE' and r.visibility = 'SOCIAL_ON'))
            order by m.handle
            """)
    List<MemberSearchResponse> searchByHandlePrefix(
            @Param("viewerId") Long viewerId,
            @Param("prefix") String prefix,
            Limit limit
    );
}

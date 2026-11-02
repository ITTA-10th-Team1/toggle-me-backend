package com.itta.toggleme.image.repository;

import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageAnalysisRepository extends JpaRepository<ImageAnalysis, Long> {

    Optional<ImageAnalysis> findByIdAndMemberId(Long id, Long memberId);

    // 같은 사진의 끝난 분석이나 아직 만료되지 않은 진행 중 분석. 최근 것부터
    @Query("""
            select analysis from ImageAnalysis analysis
            where analysis.member.id = :memberId
              and analysis.imageKey = :imageKey
              and (analysis.status = com.itta.toggleme.image.domain.ImageAnalysisStatus.COMPLETED
                   or (analysis.status = com.itta.toggleme.image.domain.ImageAnalysisStatus.PENDING
                       and analysis.createdAt > :pendingSince))
            order by analysis.id desc
            """)
    List<ImageAnalysis> findReusable(
            @Param("memberId") Long memberId,
            @Param("imageKey") String imageKey,
            @Param("pendingSince") Instant pendingSince
    );

    long countByMemberIdAndStatusAndCreatedAtAfter(Long memberId, ImageAnalysisStatus status, Instant createdAt);
}

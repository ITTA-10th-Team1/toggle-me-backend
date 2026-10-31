package com.itta.toggleme.image.repository;

import com.itta.toggleme.image.domain.ImageAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageAnalysisRepository extends JpaRepository<ImageAnalysis, Long> {

    Optional<ImageAnalysis> findByIdAndMemberId(Long id, Long memberId);
}

package com.itta.toggleme.image.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import com.itta.toggleme.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAnalysis extends BaseTimeEntity {

    // 앱은 이 시간까지만 결과를 기다리고, 넘으면 태그 없이 기록을 이어간다
    private static final Duration PENDING_TIMEOUT = Duration.ofSeconds(60);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageAnalysisStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private ImageAnalysisResult result;

    private ImageAnalysis(Member member, String imageKey) {
        this.member = member;
        this.imageKey = imageKey;
        this.status = ImageAnalysisStatus.PENDING;
    }

    public static ImageAnalysis request(Member member, String imageKey) {
        return new ImageAnalysis(member, imageKey);
    }

    public void complete(ImageAnalysisResult result) {
        this.status = ImageAnalysisStatus.COMPLETED;
        this.result = result;
    }

    public void fail() {
        this.status = ImageAnalysisStatus.FAILED;
    }

    // 서버 재시작이나 대기열 거절로 처리되지 못한 요청도 60초가 지나면 실패로 보인다
    public ImageAnalysisStatus currentStatus(Instant now) {
        if (status == ImageAnalysisStatus.PENDING && getCreatedAt().plus(PENDING_TIMEOUT).isBefore(now)) {
            return ImageAnalysisStatus.FAILED;
        }
        return status;
    }
}

package com.itta.toggleme.member.domain;

import com.itta.toggleme.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String handle;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 500)
    private String profileObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(nullable = false)
    private boolean onboardingCompleted;

    private Member(String handle, String nickname) {
        this.handle = handle;
        this.nickname = nickname;
        this.status = MemberStatus.ACTIVE;
        this.onboardingCompleted = false;
    }

    public static Member create(String handle, String nickname) {
        return new Member(handle, nickname);
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }
}

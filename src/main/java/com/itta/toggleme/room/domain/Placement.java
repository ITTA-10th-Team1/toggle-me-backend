package com.itta.toggleme.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 가구 배치. 프론트 GLB 장면 좌표(오른손 좌표계, +Y가 위쪽, 바닥은 X·Z)를 그대로 저장한다.
 * position은 그룹 노드 원점, rotation은 쿼터니언(X, Y, Z, W), scale은 축별 배율이다.
 * 프론트는 이 값으로 노드의 TRS를 절대값으로 교체한다(T × R × S).
 */
@Embeddable
public record Placement(
        @Column(name = "position_x") Double positionX,
        @Column(name = "position_y") Double positionY,
        @Column(name = "position_z") Double positionZ,
        @Column(name = "rotation_x") Double rotationX,
        @Column(name = "rotation_y") Double rotationY,
        @Column(name = "rotation_z") Double rotationZ,
        @Column(name = "rotation_w") Double rotationW,
        @Column(name = "scale_x") Double scaleX,
        @Column(name = "scale_y") Double scaleY,
        @Column(name = "scale_z") Double scaleZ
) {

    // 기준 데이터를 넣다가 값 하나만 빠져도 방 생성·조회에서 원인 모를 500이 나지 않게 10개 모두 있어야 쓴다
    public boolean isComplete() {
        return Stream.of(positionX, positionY, positionZ, rotationX, rotationY, rotationZ, rotationW,
                        scaleX, scaleY, scaleZ)
                .allMatch(Objects::nonNull);
    }

    // 회전 없음, 크기 1
    public static Placement at(double x, double y, double z) {
        return new Placement(x, y, z, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0);
    }
}

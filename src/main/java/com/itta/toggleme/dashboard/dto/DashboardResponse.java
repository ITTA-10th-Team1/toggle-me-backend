package com.itta.toggleme.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        Instant generatedAt,
        int days,
        Summary summary,
        Funnel funnel,
        Analysis imageAnalysis,
        List<FurnitureItems> furnitureItems,
        List<Daily> daily
) {

    public record Summary(long members, long rooms, long items, long follows) {
    }

    // 기간 안에 가입한 회원이 단계마다 몇 명 남는지. returned는 서로 다른 날짜에 2일 이상 기록한 회원
    public record Funnel(long signedUp, long profileCompleted, long recorded, long returned) {
    }

    // 기간 안에 요청된 AI 이미지 분석. stuck은 60초가 지나도 PENDING인 건(앱은 실패로 보여 준다)
    // 소요 시간은 완료된 건만 계산하고, 완료가 없으면 null이다
    public record Analysis(long total, long completed, long failed, long pending, long stuck,
                           Double avgSeconds, Double maxSeconds) {
    }

    public record FurnitureItems(String code, String name, long items) {
    }

    public record Daily(LocalDate date, long members, long items) {
    }
}

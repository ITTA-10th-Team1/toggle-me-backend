package com.itta.toggleme.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        Instant generatedAt,
        int days,
        Summary summary,
        List<Daily> daily
) {

    public record Summary(long members, long rooms, long items, long follows) {
    }

    public record Daily(LocalDate date, long members, long items) {
    }
}

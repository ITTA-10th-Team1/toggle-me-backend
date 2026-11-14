package com.itta.toggleme.dashboard.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.dashboard.dto.DashboardResponse;
import com.itta.toggleme.dashboard.service.DashboardService;
import com.itta.toggleme.global.security.DashboardSecurityConfig;
import com.itta.toggleme.global.security.PasswordEncoderConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({DashboardSecurityConfig.class, PasswordEncoderConfig.class})
@TestPropertySource(properties = {
        "admin.dashboard.username=dashboard",
        "admin.dashboard.password=secret"
})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        when(dashboardService.getDashboard(anyInt())).thenReturn(new DashboardResponse(
                Instant.parse("2026-11-14T00:00:00Z"),
                14,
                new DashboardResponse.Summary(10, 8, 30, 4),
                List.of(new DashboardResponse.Daily(LocalDate.of(2026, 11, 14), 2, 5))
        ));
    }

    @Test
    void 관리자_계정으로_대시보드_API와_화면에_접근한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").with(httpBasic("dashboard", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.members").value(10))
                .andExpect(jsonPath("$.daily[0].items").value(5));

        mockMvc.perform(get("/admin.html").with(httpBasic("dashboard", "secret")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"members\"")));
    }

    @Test
    void 인증이_없거나_틀리면_대시보드_접근을_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin.html").with(httpBasic("dashboard", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 조회_기간은_1일부터_90일까지다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .param("days", "91")
                        .with(httpBasic("dashboard", "secret")))
                .andExpect(status().isBadRequest());
    }
}

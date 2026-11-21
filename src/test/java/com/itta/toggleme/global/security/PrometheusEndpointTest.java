package com.itta.toggleme.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "admin.dashboard.username=dashboard",
        "admin.dashboard.password=secret"
})
@AutoConfigureMockMvc
@AutoConfigureMetrics
class PrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 관리자_계정으로만_Prometheus_지표를_가져간다() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(httpBasic("dashboard", "secret")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("jvm_memory_used_bytes")));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void health와_prometheus_외_actuator는_로그인해도_열리지_않는다() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(1L);

        mockMvc.perform(get("/actuator/env").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}

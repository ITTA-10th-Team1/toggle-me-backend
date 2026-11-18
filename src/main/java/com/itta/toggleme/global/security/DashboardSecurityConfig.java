package com.itta.toggleme.global.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class DashboardSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain dashboardSecurityFilterChain(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            @Value("${admin.dashboard.username:}") String username,
            @Value("${admin.dashboard.password:}") String password
    ) throws Exception {
        List<UserDetails> users = new ArrayList<>();
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            users.add(User.withUsername(username)
                    .password(passwordEncoder.encode(password))
                    .roles("DASHBOARD")
                    .build());
        }

        http
                .securityMatcher("/admin.html", "/api/v1/admin/dashboard")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("DASHBOARD"))
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(new InMemoryUserDetailsManager(users))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}

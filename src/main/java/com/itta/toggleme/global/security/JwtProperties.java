package com.itta.toggleme.global.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenExpiry,
        Duration refreshTokenExpiry
) {
}

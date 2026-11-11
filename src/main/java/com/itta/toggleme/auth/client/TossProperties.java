package com.itta.toggleme.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss")
public record TossProperties(
        String apiBaseUrl,
        String certPath,
        String keyPath
) {

    boolean hasMtlsFiles() {
        return certPath != null && !certPath.isBlank() && keyPath != null && !keyPath.isBlank();
    }
}

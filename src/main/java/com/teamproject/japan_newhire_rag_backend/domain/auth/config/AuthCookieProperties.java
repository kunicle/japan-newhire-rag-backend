package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cookie")
public record AuthCookieProperties(
        String name,
        boolean secure,
        String sameSite,
        String path,
        Duration maxAge
) {

    public AuthCookieProperties {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("auth.cookie.name must not be blank");
        }
        if (sameSite == null || sameSite.isBlank()) {
            throw new IllegalArgumentException("auth.cookie.same-site must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("auth.cookie.path must not be blank");
        }
        if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("auth.cookie.max-age must be positive");
        }
    }
}

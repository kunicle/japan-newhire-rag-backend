package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.token")
public record AuthTokenProperties(
        String secret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration
) {

    public AuthTokenProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("auth.token.secret must not be blank");
        }
        if (accessTokenExpiration == null || accessTokenExpiration.isNegative()
                || accessTokenExpiration.isZero()) {
            throw new IllegalArgumentException(
                    "auth.token.access-token-expiration must be positive");
        }
        if (refreshTokenExpiration == null || refreshTokenExpiration.isNegative()
                || refreshTokenExpiration.isZero()) {
            throw new IllegalArgumentException(
                    "auth.token.refresh-token-expiration must be positive");
        }
    }
}

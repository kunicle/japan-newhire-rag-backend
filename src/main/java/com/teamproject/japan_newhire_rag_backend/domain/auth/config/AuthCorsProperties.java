package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cors")
public record AuthCorsProperties(List<String> allowedOrigins) {

    public AuthCorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("auth.cors.allowed-origins must not be empty");
        }
        allowedOrigins = allowedOrigins.stream()
                .map(AuthCorsProperties::validateAndNormalizeOrigin)
                .toList();
    }

    private static String validateAndNormalizeOrigin(String origin) {
        if (origin == null) {
            throw invalidOrigin(null);
        }

        String normalized = origin.trim();
        if (normalized.isEmpty() || normalized.contains("*")) {
            throw invalidOrigin(origin);
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            String path = uri.getRawPath();
            boolean valid = ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && (path == null || path.isEmpty() || "/".equals(path));
            if (!valid) {
                throw invalidOrigin(origin);
            }
            return normalized;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Invalid auth.cors.allowed-origins value: " + origin,
                    exception);
        }
    }

    private static IllegalArgumentException invalidOrigin(String origin) {
        return new IllegalArgumentException(
                "Invalid auth.cors.allowed-origins value: " + origin);
    }
}

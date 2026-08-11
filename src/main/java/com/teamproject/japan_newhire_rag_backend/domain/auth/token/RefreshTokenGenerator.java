package com.teamproject.japan_newhire_rag_backend.domain.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthTokenProperties;

@Component
public class RefreshTokenGenerator {

    private static final int TOKEN_RANDOM_BYTES = 32;

    private final SecureRandom secureRandom;
    private final Clock clock;
    private final AuthTokenProperties properties;

    public RefreshTokenGenerator(Clock clock, AuthTokenProperties properties) {
        this.secureRandom = new SecureRandom();
        this.clock = clock;
        this.properties = properties;
    }

    public IssuedRefreshToken issue() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        return new IssuedRefreshToken(
                rawToken,
                hash(rawToken),
                LocalDateTime.now(clock).plus(properties.refreshTokenExpiration()));
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken must not be blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

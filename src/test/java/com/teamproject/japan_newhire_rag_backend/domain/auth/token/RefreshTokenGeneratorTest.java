package com.teamproject.japan_newhire_rag_backend.domain.auth.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.auth.config.AuthTokenProperties;

class RefreshTokenGeneratorTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 8, 11, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            ISSUED_AT.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);
    private static final AuthTokenProperties PROPERTIES = new AuthTokenProperties(
            "0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15),
            Duration.ofDays(14));

    @Test
    void issueCreatesAtLeast256BitsOfUrlSafeRandomDataAndFourteenDayExpiration() {
        RefreshTokenGenerator generator = new RefreshTokenGenerator(FIXED_CLOCK, PROPERTIES);

        IssuedRefreshToken issued = generator.issue();
        byte[] decoded = Base64.getUrlDecoder().decode(issued.rawToken());

        assertTrue(decoded.length >= 32);
        assertTrue(issued.rawToken().matches("[A-Za-z0-9_-]+"));
        assertEquals(ISSUED_AT.plusDays(14), issued.expiresAt());
        assertNotEquals(issued.rawToken(), issued.tokenHash());
    }

    @Test
    void hashIsDeterministicForSameRawToken() {
        RefreshTokenGenerator generator = new RefreshTokenGenerator(FIXED_CLOCK, PROPERTIES);

        assertEquals(generator.hash("same-token"), generator.hash("same-token"));
    }

    @Test
    void independentlyIssuedTokensHaveDifferentHashes() {
        RefreshTokenGenerator generator = new RefreshTokenGenerator(FIXED_CLOCK, PROPERTIES);

        IssuedRefreshToken first = generator.issue();
        IssuedRefreshToken second = generator.issue();

        assertNotEquals(first.rawToken(), second.rawToken());
        assertNotEquals(first.tokenHash(), second.tokenHash());
    }
}

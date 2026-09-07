package com.teamproject.japan_newhire_rag_backend.domain.auth.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 12, 0);

    @Test
    void issueCreatesActiveRefreshTokenWithRequiredValues() {
        AppUser appUser = new AppUser();

        RefreshToken token = RefreshToken.issue(
                appUser,
                "token-hash",
                NOW.plusDays(14),
                "test-device");

        assertEquals(appUser, token.getAppUser());
        assertEquals("token-hash", token.getTokenHash());
        assertEquals(NOW.plusDays(14), token.getExpiresAt());
        assertEquals("test-device", token.getDeviceInfo());
        assertNull(token.getRevokedAt());
        assertFalse(token.isRevoked());
    }

    @Test
    void revokeMarksRefreshTokenAsRevoked() {
        RefreshToken token = RefreshToken.issue(
                new AppUser(), "token-hash", NOW.plusDays(14), null);

        token.revoke(NOW);

        assertTrue(token.isRevoked());
        assertEquals(NOW, token.getRevokedAt());
    }

    @Test
    void expirationIncludesExactExpirationInstant() {
        RefreshToken token = RefreshToken.issue(
                new AppUser(), "token-hash", NOW.plusDays(14), null);

        assertFalse(token.isExpired(NOW.plusDays(14).minusNanos(1)));
        assertTrue(token.isExpired(NOW.plusDays(14)));
        assertTrue(token.isExpired(NOW.plusDays(15)));
    }
}

package com.teamproject.japan_newhire_rag_backend.domain.auth.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;

class AppUserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 12, 0);

    @Test
    void firstFourPasswordFailuresIncreaseCountWithoutLocking() {
        AppUser appUser = new AppUser();

        for (int attempt = 1; attempt <= 4; attempt++) {
            appUser.recordLoginFailure(NOW);
            assertEquals(attempt, appUser.getFailedLoginCount());
            assertNull(appUser.getLockedUntil());
        }
    }

    @Test
    void fifthPasswordFailureLocksAccountForTenMinutes() {
        AppUser appUser = new AppUser();

        for (int attempt = 1; attempt <= 5; attempt++) {
            appUser.recordLoginFailure(NOW);
        }

        assertEquals(5, appUser.getFailedLoginCount());
        assertEquals(AccountStatus.LOCKED, appUser.getAccountStatus());
        assertEquals(NOW.plusMinutes(10), appUser.getLockedUntil());
    }

    @Test
    void successfulLoginClearsFailureStateAndUpdatesLastLoginTime() {
        AppUser appUser = new AppUser();
        appUser.recordLoginFailure(NOW.minusMinutes(1));

        appUser.recordLoginSuccess(NOW);

        assertEquals(0, appUser.getFailedLoginCount());
        assertNull(appUser.getLockedUntil());
        assertEquals(NOW, appUser.getLastLoginAt());
    }

    @Test
    void expiredLockIsActivatedAndFailureCountIsReset() {
        AppUser appUser = new AppUser();
        for (int attempt = 1; attempt <= 5; attempt++) {
            appUser.recordLoginFailure(NOW.minusMinutes(10));
        }

        assertTrue(appUser.unlockIfExpired(NOW));
        assertEquals(AccountStatus.ACTIVE, appUser.getAccountStatus());
        assertEquals(0, appUser.getFailedLoginCount());
        assertNull(appUser.getLockedUntil());
        assertFalse(appUser.unlockIfExpired(NOW));
    }
}

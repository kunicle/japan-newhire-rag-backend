package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.RefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RefreshTokenRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

@ExtendWith(MockitoExtension.class)
class RefreshTokenLogoutServiceTest {

    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String TOKEN_HASH = "refresh-token-hash";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private RefreshToken refreshToken;

    @Mock
    private RefreshToken otherDeviceToken;

    private RefreshTokenLogoutService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenLogoutService(
                refreshTokenRepository,
                refreshTokenGenerator,
                FIXED_CLOCK);
    }

    @Test
    void logoutHashesAndLocksCurrentTokenThenRevokesOnlyThatToken() {
        stubFoundToken(false);

        service.logout(RAW_TOKEN);

        verify(refreshTokenGenerator).hash(RAW_TOKEN);
        verify(refreshTokenRepository).findForUpdateByTokenHash(TOKEN_HASH);
        verify(refreshToken).revoke(NOW);
        verifyNoInteractions(otherDeviceToken);
    }

    @Test
    void logoutTreatsAlreadyRevokedTokenAsSuccessfulNoOp() {
        stubFoundToken(true);

        assertDoesNotThrow(() -> service.logout(RAW_TOKEN));

        verify(refreshToken, never()).revoke(NOW);
    }

    @Test
    void logoutRevokesExpiredTokenWhenItIsNotAlreadyRevoked() {
        RefreshToken expiredToken = RefreshToken.issue(
                mock(AppUser.class),
                TOKEN_HASH,
                NOW.minusSeconds(1),
                null);
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findForUpdateByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(expiredToken));

        assertDoesNotThrow(() -> service.logout(RAW_TOKEN));

        assertTrue(expiredToken.isRevoked());
        assertEquals(NOW, expiredToken.getRevokedAt());
    }

    @Test
    void logoutTreatsUnknownTokenAsSuccessfulNoOp() {
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findForUpdateByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.logout(RAW_TOKEN));

        verifyNoInteractions(refreshToken, otherDeviceToken);
    }

    @Test
    void logoutRejectsNullOrBlankTokenWithoutRepositoryAccess() {
        assertThrows(BadCredentialsException.class, () -> service.logout(null));
        assertThrows(BadCredentialsException.class, () -> service.logout("   "));

        verifyNoInteractions(refreshTokenGenerator, refreshTokenRepository);
    }

    private void stubFoundToken(boolean revoked) {
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findForUpdateByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(refreshToken));
        when(refreshToken.isRevoked()).thenReturn(revoked);
    }
}

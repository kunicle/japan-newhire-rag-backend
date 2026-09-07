package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.RefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RefreshTokenRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.IssuedRefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRotationServiceTest {

    private static final String RAW_TOKEN = "current-raw-refresh-token";
    private static final String CURRENT_HASH = "current-token-hash";
    private static final String NEW_RAW_TOKEN = "new-raw-refresh-token";
    private static final String NEW_HASH = "new-token-hash";
    private static final String ACCESS_TOKEN = "new-access-token";
    private static final String DEVICE_INFO = "existing-device";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 12, 0);
    private static final LocalDateTime NEW_EXPIRES_AT = NOW.plusDays(14);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private RefreshToken currentToken;

    @Mock
    private AppUser appUser;

    private RefreshTokenRotationService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenRotationService(
                refreshTokenRepository,
                refreshTokenGenerator,
                accessTokenService,
                FIXED_CLOCK);
    }

    @Test
    void rotateRevokesCurrentTokenAndIssuesAndStoresNewTokenPair() {
        stubValidCurrentToken();

        LoginTokenPair result = service.rotate(RAW_TOKEN);

        assertEquals(ACCESS_TOKEN, result.accessToken());
        assertEquals(NEW_RAW_TOKEN, result.refreshToken());
        verify(refreshTokenGenerator).hash(RAW_TOKEN);
        verify(refreshTokenRepository).findForUpdateByTokenHash(CURRENT_HASH);
        verify(currentToken).revoke(NOW);
        verify(accessTokenService).issue(1L);
        verify(refreshTokenGenerator).issue();

        RefreshToken saved = captureSavedRefreshToken();
        assertSame(appUser, saved.getAppUser());
        assertEquals(NEW_HASH, saved.getTokenHash());
        assertEquals(NEW_EXPIRES_AT, saved.getExpiresAt());
        assertEquals(DEVICE_INFO, saved.getDeviceInfo());
        assertNotEquals(NEW_RAW_TOKEN, saved.getTokenHash());
    }

    @Test
    void rotateRejectsUnknownTokenUsingPessimisticLockLookup() {
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(CURRENT_HASH);
        when(refreshTokenRepository.findForUpdateByTokenHash(CURRENT_HASH))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.rotate(RAW_TOKEN));

        verify(refreshTokenRepository).findForUpdateByTokenHash(CURRENT_HASH);
        verifyNoInteractions(accessTokenService);
        verify(refreshTokenGenerator, never()).issue();
    }

    @Test
    void rotateRejectsRevokedToken() {
        stubLookup();
        when(currentToken.isRevoked()).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> service.rotate(RAW_TOKEN));
        verifyNoRotationActions();
    }

    @Test
    void rotateRejectsExpiredToken() {
        stubLookup();
        when(currentToken.isRevoked()).thenReturn(false);
        when(currentToken.isExpired(NOW)).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> service.rotate(RAW_TOKEN));
        verifyNoRotationActions();
    }

    @Test
    void rotateRejectsInactiveAccount() {
        stubTokenWithAccount(AccountStatus.INACTIVE, null);

        assertThrows(DisabledException.class, () -> service.rotate(RAW_TOKEN));
        verifyNoRotationActions();
    }

    @Test
    void rotateRejectsLockedAccount() {
        stubTokenWithAccount(AccountStatus.LOCKED, null);

        assertThrows(LockedException.class, () -> service.rotate(RAW_TOKEN));
        verifyNoRotationActions();
    }

    @Test
    void rotateRejectsDeletedAccount() {
        stubTokenWithAccount(AccountStatus.ACTIVE, NOW.minusDays(1));

        assertThrows(DisabledException.class, () -> service.rotate(RAW_TOKEN));
        verifyNoRotationActions();
    }

    @Test
    void rotateRejectsTokenWithoutAssociatedAppUser() {
        stubLookup();
        when(currentToken.isRevoked()).thenReturn(false);
        when(currentToken.isExpired(NOW)).thenReturn(false);
        when(currentToken.getAppUser()).thenReturn(null);

        assertThrows(AuthenticationServiceException.class, () -> service.rotate(RAW_TOKEN));
        verifyNoRotationActions();
    }

    @Test
    void rotatePropagatesNewRefreshTokenStorageFailure() {
        stubValidCurrentToken();
        DataAccessResourceFailureException storageFailure =
                new DataAccessResourceFailureException("database unavailable");
        when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenThrow(storageFailure);

        DataAccessResourceFailureException thrown = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.rotate(RAW_TOKEN));

        assertSame(storageFailure, thrown);
        verify(currentToken).revoke(NOW);
    }

    private void stubValidCurrentToken() {
        stubTokenWithAccount(AccountStatus.ACTIVE, null);
        when(appUser.getAppUserId()).thenReturn(1L);
        when(currentToken.getDeviceInfo()).thenReturn(DEVICE_INFO);
        when(accessTokenService.issue(1L)).thenReturn(ACCESS_TOKEN);
        when(refreshTokenGenerator.issue()).thenReturn(new IssuedRefreshToken(
                NEW_RAW_TOKEN,
                NEW_HASH,
                NEW_EXPIRES_AT));
    }

    private void stubTokenWithAccount(
            AccountStatus accountStatus,
            LocalDateTime deletedAt
    ) {
        stubLookup();
        when(currentToken.isRevoked()).thenReturn(false);
        when(currentToken.isExpired(NOW)).thenReturn(false);
        when(currentToken.getAppUser()).thenReturn(appUser);
        when(appUser.getDeletedAt()).thenReturn(deletedAt);
        lenient().when(appUser.getAccountStatus()).thenReturn(accountStatus);
    }

    private void stubLookup() {
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(CURRENT_HASH);
        when(refreshTokenRepository.findForUpdateByTokenHash(CURRENT_HASH))
                .thenReturn(Optional.of(currentToken));
    }

    private void verifyNoRotationActions() {
        verify(currentToken, never()).revoke(NOW);
        verifyNoInteractions(accessTokenService);
        verify(refreshTokenGenerator, never()).issue();
        verify(refreshTokenRepository, never()).save(
                org.mockito.ArgumentMatchers.any(RefreshToken.class));
    }

    private RefreshToken captureSavedRefreshToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }
}

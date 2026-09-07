package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.BadCredentialsException;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.RefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RefreshTokenRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.IssuedRefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

@ExtendWith(MockitoExtension.class)
class LoginTokenOrchestrationServiceTest {

    private static final String EMAIL = "employee@example.com";
    private static final String RAW_PASSWORD = "raw-password";
    private static final String DEVICE_INFO = "test-device";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";
    private static final String REFRESH_TOKEN_HASH = "refresh-token-hash";
    private static final LocalDateTime REFRESH_EXPIRES_AT =
            LocalDateTime.of(2026, 8, 25, 12, 0);

    @Mock
    private InternalLoginAuthenticationService loginAuthenticationService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AppUser appUser;

    private LoginTokenOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new LoginTokenOrchestrationService(
                loginAuthenticationService,
                appUserRepository,
                accessTokenService,
                refreshTokenGenerator,
                refreshTokenRepository);
    }

    @Test
    void loginAuthenticatesIssuesTokensAndStoresOnlyRefreshTokenHash() {
        stubSuccessfulFlow();

        LoginTokenPair result = service.login(EMAIL, RAW_PASSWORD, DEVICE_INFO);

        assertEquals(ACCESS_TOKEN, result.accessToken());
        assertEquals(RAW_REFRESH_TOKEN, result.refreshToken());
        verify(loginAuthenticationService).authenticate(EMAIL, RAW_PASSWORD);
        verify(accessTokenService).issue(1L);
        verify(refreshTokenGenerator).issue();

        RefreshToken saved = captureSavedRefreshToken();
        assertSame(appUser, saved.getAppUser());
        assertEquals(REFRESH_TOKEN_HASH, saved.getTokenHash());
        assertEquals(REFRESH_EXPIRES_AT, saved.getExpiresAt());
        assertEquals(DEVICE_INFO, saved.getDeviceInfo());
        assertNotEquals(RAW_REFRESH_TOKEN, saved.getTokenHash());
    }

    @Test
    void loginAllowsNullDeviceInfo() {
        stubSuccessfulFlow();

        service.login(EMAIL, RAW_PASSWORD, null);

        assertEquals(null, captureSavedRefreshToken().getDeviceInfo());
    }

    @Test
    void loginDoesNotIssueOrStoreTokensWhenAuthenticationFails() {
        when(loginAuthenticationService.authenticate(EMAIL, RAW_PASSWORD))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        assertThrows(
                BadCredentialsException.class,
                () -> service.login(EMAIL, RAW_PASSWORD, DEVICE_INFO));

        verifyNoInteractions(
                appUserRepository,
                accessTokenService,
                refreshTokenGenerator,
                refreshTokenRepository);
    }

    @Test
    void loginFailsClearlyWhenAuthenticatedAppUserDisappears() {
        when(loginAuthenticationService.authenticate(EMAIL, RAW_PASSWORD)).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.login(EMAIL, RAW_PASSWORD, DEVICE_INFO));

        assertEquals("Authenticated AppUser no longer exists: 1", exception.getMessage());
        verifyNoInteractions(
                accessTokenService,
                refreshTokenGenerator,
                refreshTokenRepository);
    }

    @Test
    void loginPropagatesRefreshTokenStorageFailure() {
        stubSuccessfulFlow();
        DataAccessResourceFailureException storageFailure =
                new DataAccessResourceFailureException("database unavailable");
        when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenThrow(storageFailure);

        DataAccessResourceFailureException thrown = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.login(EMAIL, RAW_PASSWORD, DEVICE_INFO));

        assertSame(storageFailure, thrown);
    }

    private void stubSuccessfulFlow() {
        when(loginAuthenticationService.authenticate(EMAIL, RAW_PASSWORD)).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(accessTokenService.issue(1L)).thenReturn(ACCESS_TOKEN);
        when(refreshTokenGenerator.issue()).thenReturn(new IssuedRefreshToken(
                RAW_REFRESH_TOKEN,
                REFRESH_TOKEN_HASH,
                REFRESH_EXPIRES_AT));
    }

    private RefreshToken captureSavedRefreshToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }
}

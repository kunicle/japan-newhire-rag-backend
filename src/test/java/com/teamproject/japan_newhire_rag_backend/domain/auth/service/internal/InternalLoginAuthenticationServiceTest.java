package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.LoginAttempt;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.LoginFailureReason;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.LoginResult;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.LoginAttemptRepository;

@ExtendWith(MockitoExtension.class)
class InternalLoginAuthenticationServiceTest {

    private static final String EMAIL = "employee@example.com";
    private static final String RAW_PASSWORD = "raw-password";
    private static final String PASSWORD_HASH = "encoded-password";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-11T12:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private InternalLoginAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new InternalLoginAuthenticationService(
                appUserRepository,
                loginAttemptRepository,
                passwordEncoder,
                FIXED_CLOCK);
    }

    @Test
    void authenticateRecordsSuccessAndReturnsAppUserId() {
        AppUser appUser = appUser(1L, AccountStatus.ACTIVE, null);
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertEquals(1L, service.authenticate(EMAIL, RAW_PASSWORD));

        verify(appUser).recordLoginSuccess(NOW);
        LoginAttempt attempt = capturedAttempt();
        assertEquals(LoginResult.SUCCESS, attempt.getLoginResult());
        assertEquals(appUser, attempt.getAppUser());
        assertEquals(EMAIL, attempt.getInputEmail());
        assertEquals(NOW, attempt.getAttemptedAt());
        assertNull(attempt.getFailureReason());
    }

    @Test
    void authenticateRecordsPasswordFailureAndIncrementsFailureState() {
        AppUser appUser = appUser(1L, AccountStatus.ACTIVE, null);
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> service.authenticate(EMAIL, RAW_PASSWORD));

        verify(appUser).recordLoginFailure(NOW);
        LoginAttempt attempt = capturedAttempt();
        assertEquals(LoginResult.FAILURE, attempt.getLoginResult());
        assertEquals(LoginFailureReason.INVALID_CREDENTIALS, attempt.getFailureReason());
        assertEquals(appUser, attempt.getAppUser());
    }

    @Test
    void authenticateRecordsFailureWithoutAppUserWhenEmailDoesNotExist() {
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> service.authenticate(EMAIL, RAW_PASSWORD));

        LoginAttempt attempt = capturedAttempt();
        assertEquals(LoginResult.FAILURE, attempt.getLoginResult());
        assertEquals(LoginFailureReason.INVALID_CREDENTIALS, attempt.getFailureReason());
        assertNull(attempt.getAppUser());
        assertEquals(EMAIL, attempt.getInputEmail());
    }

    @Test
    void authenticateRejectsUnexpiredLockedAccountAndRecordsFailure() {
        AppUser appUser = appUser(1L, AccountStatus.LOCKED, null);
        when(appUser.unlockIfExpired(NOW)).thenReturn(false);
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(appUser));

        assertThrows(LockedException.class, () -> service.authenticate(EMAIL, RAW_PASSWORD));

        verify(passwordEncoder, never()).matches(RAW_PASSWORD, PASSWORD_HASH);
        LoginAttempt attempt = capturedAttempt();
        assertEquals(LoginFailureReason.LOCKED, attempt.getFailureReason());
    }

    @Test
    void authenticateUnlocksExpiredAccountAndContinuesLogin() {
        AppUser appUser = appUser(1L, AccountStatus.LOCKED, null);
        when(appUser.unlockIfExpired(NOW)).thenReturn(true);
        when(appUser.getAccountStatus()).thenReturn(AccountStatus.LOCKED, AccountStatus.ACTIVE);
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        assertEquals(1L, service.authenticate(EMAIL, RAW_PASSWORD));

        verify(appUser).unlockIfExpired(NOW);
        verify(appUser).recordLoginSuccess(NOW);
    }

    @Test
    void authenticateRejectsInactiveAccountAndRecordsFailure() {
        AppUser appUser = appUser(1L, AccountStatus.INACTIVE, null);
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(appUser));

        assertThrows(DisabledException.class, () -> service.authenticate(EMAIL, RAW_PASSWORD));

        verify(passwordEncoder, never()).matches(RAW_PASSWORD, PASSWORD_HASH);
        LoginAttempt attempt = capturedAttempt();
        assertEquals(LoginFailureReason.INACTIVE, attempt.getFailureReason());
    }

    @Test
    void authenticateRejectsDeletedAccountAndRecordsInactiveFailure() {
        AppUser appUser = appUser(1L, AccountStatus.ACTIVE, NOW.minusDays(1));
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(appUser));

        assertThrows(DisabledException.class, () -> service.authenticate(EMAIL, RAW_PASSWORD));

        LoginAttempt attempt = capturedAttempt();
        assertEquals(LoginFailureReason.INACTIVE, attempt.getFailureReason());
    }

    private AppUser appUser(
            Long appUserId,
            AccountStatus accountStatus,
            LocalDateTime deletedAt
    ) {
        AppUser appUser = mock(AppUser.class);
        lenient().when(appUser.getAppUserId()).thenReturn(appUserId);
        lenient().when(appUser.getPasswordHash()).thenReturn(PASSWORD_HASH);
        lenient().when(appUser.getAccountStatus()).thenReturn(accountStatus);
        lenient().when(appUser.getDeletedAt()).thenReturn(deletedAt);
        return appUser;
    }

    private LoginAttempt capturedAttempt() {
        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        return captor.getValue();
    }
}

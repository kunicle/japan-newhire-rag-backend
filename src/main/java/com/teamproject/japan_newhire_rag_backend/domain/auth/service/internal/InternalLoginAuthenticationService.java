package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.LoginAttempt;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.LoginFailureReason;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.LoginAttemptRepository;

@Service
@Transactional(noRollbackFor = AuthenticationException.class)
public class InternalLoginAuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final AppUserRepository appUserRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public InternalLoginAuthenticationService(
            AppUserRepository appUserRepository,
            LoginAttemptRepository loginAttemptRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.appUserRepository = appUserRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public Long authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        LocalDateTime attemptedAt = LocalDateTime.now(clock);
        AppUser appUser = appUserRepository.findForUpdateByEmail(email).orElse(null);

        if (appUser == null) {
            recordFailure(null, email, LoginFailureReason.INVALID_CREDENTIALS, attemptedAt);
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        validateLoginAvailable(appUser, email, attemptedAt);

        if (!passwordEncoder.matches(rawPassword, appUser.getPasswordHash())) {
            appUser.recordLoginFailure(attemptedAt);
            recordFailure(
                    appUser,
                    email,
                    LoginFailureReason.INVALID_CREDENTIALS,
                    attemptedAt);
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        appUser.recordLoginSuccess(attemptedAt);
        loginAttemptRepository.save(LoginAttempt.success(appUser, email, attemptedAt));
        return appUser.getAppUserId();
    }

    private void validateLoginAvailable(
            AppUser appUser,
            String email,
            LocalDateTime attemptedAt
    ) {
        if (appUser.getDeletedAt() != null) {
            recordFailure(appUser, email, LoginFailureReason.INACTIVE, attemptedAt);
            throw new DisabledException("Account is not available");
        }

        if (appUser.getAccountStatus() == AccountStatus.LOCKED
                && !appUser.unlockIfExpired(attemptedAt)) {
            recordFailure(appUser, email, LoginFailureReason.LOCKED, attemptedAt);
            throw new LockedException("Account is locked");
        }

        if (appUser.getAccountStatus() != AccountStatus.ACTIVE) {
            recordFailure(appUser, email, LoginFailureReason.INACTIVE, attemptedAt);
            throw new DisabledException("Account is inactive");
        }
    }

    private void recordFailure(
            AppUser appUser,
            String email,
            LoginFailureReason failureReason,
            LocalDateTime attemptedAt
    ) {
        loginAttemptRepository.save(
                LoginAttempt.failure(appUser, email, failureReason, attemptedAt));
    }
}

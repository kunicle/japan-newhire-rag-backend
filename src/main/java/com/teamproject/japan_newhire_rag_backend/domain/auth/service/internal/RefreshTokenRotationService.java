package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.RefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.AccountStatus;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RefreshTokenRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.IssuedRefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

@Service
@Transactional
public class RefreshTokenRotationService {

    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenService accessTokenService;
    private final Clock clock;

    public RefreshTokenRotationService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenService accessTokenService,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenService = accessTokenService;
        this.clock = clock;
    }

    public LoginTokenPair rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository
                .findForUpdateByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException(
                        INVALID_REFRESH_TOKEN_MESSAGE));
        LocalDateTime currentTime = LocalDateTime.now(clock);

        validateRefreshToken(currentToken, currentTime);

        AppUser appUser = currentToken.getAppUser();
        Long appUserId = appUser.getAppUserId();
        String deviceInfo = currentToken.getDeviceInfo();

        currentToken.revoke(currentTime);
        String accessToken = accessTokenService.issue(appUserId);
        IssuedRefreshToken issuedRefreshToken = refreshTokenGenerator.issue();
        RefreshToken newRefreshToken = RefreshToken.issue(
                appUser,
                issuedRefreshToken.tokenHash(),
                issuedRefreshToken.expiresAt(),
                deviceInfo);
        refreshTokenRepository.save(newRefreshToken);

        return new LoginTokenPair(accessToken, issuedRefreshToken.rawToken());
    }

    private void validateRefreshToken(
            RefreshToken refreshToken,
            LocalDateTime currentTime
    ) {
        if (refreshToken.isRevoked() || refreshToken.isExpired(currentTime)) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        AppUser appUser = refreshToken.getAppUser();
        if (appUser == null) {
            throw new AuthenticationServiceException(
                    "RefreshToken has no associated AppUser");
        }
        if (appUser.getDeletedAt() != null) {
            throw new DisabledException("Account is not available");
        }
        if (appUser.getAccountStatus() == AccountStatus.LOCKED) {
            throw new LockedException("Account is locked");
        }
        if (appUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Account is inactive");
        }
    }
}

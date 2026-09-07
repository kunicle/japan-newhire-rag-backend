package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.RefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RefreshTokenRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

@Service
@Transactional
public class RefreshTokenLogoutService {

    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid refresh token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final Clock clock;

    public RefreshTokenLogoutService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.clock = clock;
    }

    public void logout(Long currentAppUserId, String rawRefreshToken) {
        if (currentAppUserId == null) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        String tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepository.findForUpdateByTokenHash(tokenHash)
                .ifPresent(refreshToken -> revokeOwnedToken(currentAppUserId, refreshToken));
    }

    private void revokeOwnedToken(Long currentAppUserId, RefreshToken refreshToken) {
        if (refreshToken.getAppUser() == null
                || !Objects.equals(
                        currentAppUserId,
                        refreshToken.getAppUser().getAppUserId())) {
            throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
        }
        if (!refreshToken.isRevoked()) {
            revoke(refreshToken);
        }
    }

    private void revoke(RefreshToken refreshToken) {
        refreshToken.revoke(LocalDateTime.now(clock));
    }
}

package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.RefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.RefreshTokenRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.AccessTokenService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.IssuedRefreshToken;
import com.teamproject.japan_newhire_rag_backend.domain.auth.token.RefreshTokenGenerator;

@Service
@Transactional(noRollbackFor = AuthenticationException.class)
public class LoginTokenOrchestrationService {

    private final InternalLoginAuthenticationService loginAuthenticationService;
    private final AppUserRepository appUserRepository;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginTokenOrchestrationService(
            InternalLoginAuthenticationService loginAuthenticationService,
            AppUserRepository appUserRepository,
            AccessTokenService accessTokenService,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.loginAuthenticationService = loginAuthenticationService;
        this.appUserRepository = appUserRepository;
        this.accessTokenService = accessTokenService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public LoginTokenPair login(
            String email,
            String rawPassword,
            String deviceInfo
    ) {
        Long appUserId = loginAuthenticationService.authenticate(email, rawPassword);
        AppUser appUser = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated AppUser no longer exists: " + appUserId));

        String accessToken = accessTokenService.issue(appUserId);
        IssuedRefreshToken issuedRefreshToken = refreshTokenGenerator.issue();
        RefreshToken refreshToken = RefreshToken.issue(
                appUser,
                issuedRefreshToken.tokenHash(),
                issuedRefreshToken.expiresAt(),
                deviceInfo);
        refreshTokenRepository.save(refreshToken);

        return new LoginTokenPair(accessToken, issuedRefreshToken.rawToken());
    }
}

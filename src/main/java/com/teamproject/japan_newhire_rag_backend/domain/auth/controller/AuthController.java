package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.LoginRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.TokenResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.LoginTokenOrchestrationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.LoginTokenPair;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.RefreshTokenLogoutService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.RefreshTokenRotationService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginTokenOrchestrationService loginService;
    private final RefreshTokenRotationService refreshService;
    private final RefreshTokenLogoutService logoutService;
    private final CurrentUserProvider currentUserProvider;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(
            LoginTokenOrchestrationService loginService,
            RefreshTokenRotationService refreshService,
            RefreshTokenLogoutService logoutService,
            CurrentUserProvider currentUserProvider,
            RefreshTokenCookieManager refreshTokenCookieManager,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.currentUserProvider = currentUserProvider;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        LoginTokenPair tokenPair = loginService.login(
                request.email(),
                request.password(),
                request.deviceInfo());
        refreshTokenCookieManager.write(response, tokenPair.refreshToken());
        csrfTokenRepository.saveToken(
                csrfTokenRepository.generateToken(servletRequest),
                servletRequest,
                response);
        return TokenResponse.from(tokenPair);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = refreshTokenCookieManager.requireToken(request);
        LoginTokenPair tokenPair = refreshService.rotate(rawRefreshToken);
        refreshTokenCookieManager.write(response, tokenPair.refreshToken());
        return TokenResponse.from(tokenPair);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String rawRefreshToken = refreshTokenCookieManager.requireToken(request);
            Long currentAppUserId = currentUserProvider.getCurrentUser().appUserId();
            logoutService.logout(currentAppUserId, rawRefreshToken);
            return ResponseEntity.noContent().build();
        } finally {
            refreshTokenCookieManager.clear(response);
        }
    }
}

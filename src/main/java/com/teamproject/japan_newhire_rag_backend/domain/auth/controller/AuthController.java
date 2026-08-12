package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.LoginRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.RefreshTokenRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.TokenResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.LoginTokenOrchestrationService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.RefreshTokenLogoutService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.RefreshTokenRotationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginTokenOrchestrationService loginService;
    private final RefreshTokenRotationService refreshService;
    private final RefreshTokenLogoutService logoutService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(
            LoginTokenOrchestrationService loginService,
            RefreshTokenRotationService refreshService,
            RefreshTokenLogoutService logoutService,
            CurrentUserProvider currentUserProvider
    ) {
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(loginService.login(
                request.email(),
                request.password(),
                request.deviceInfo()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(refreshService.rotate(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        Long currentAppUserId = currentUserProvider.getCurrentUser().appUserId();
        logoutService.logout(currentAppUserId, request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}

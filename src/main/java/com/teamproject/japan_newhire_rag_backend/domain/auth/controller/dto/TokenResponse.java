package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.LoginTokenPair;

public record TokenResponse(
        String accessToken
) {

    public static TokenResponse from(LoginTokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken());
    }
}

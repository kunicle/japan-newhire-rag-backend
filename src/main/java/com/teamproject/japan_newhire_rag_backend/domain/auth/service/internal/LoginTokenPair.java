package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

public record LoginTokenPair(
        String accessToken,
        String refreshToken
) {
}

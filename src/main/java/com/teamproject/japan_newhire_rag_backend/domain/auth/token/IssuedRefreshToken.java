package com.teamproject.japan_newhire_rag_backend.domain.auth.token;

import java.time.LocalDateTime;

public record IssuedRefreshToken(
        String rawToken,
        String tokenHash,
        LocalDateTime expiresAt
) {
}

package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}

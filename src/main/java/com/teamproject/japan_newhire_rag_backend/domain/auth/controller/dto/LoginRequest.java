package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        String deviceInfo
) {
}

package com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal;

import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public record JwtAuthenticationUser(
        Long appUserId,
        Set<RoleType> roles
) {

    public JwtAuthenticationUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}

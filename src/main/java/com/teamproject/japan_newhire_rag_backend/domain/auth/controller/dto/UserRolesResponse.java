package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

public record UserRolesResponse(Long appUserId, Set<RoleType> roles) {
    public UserRolesResponse {
        roles = Set.copyOf(roles);
    }
}

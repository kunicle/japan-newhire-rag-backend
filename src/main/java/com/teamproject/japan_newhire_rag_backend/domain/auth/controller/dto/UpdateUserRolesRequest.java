package com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto;

import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRolesRequest(
        @NotEmpty Set<@NotNull RoleType> roles
) {
}

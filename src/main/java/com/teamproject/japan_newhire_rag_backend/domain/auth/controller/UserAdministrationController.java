package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.AccountStatusResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.CreateUserResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.UpdateUserRolesRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.UserRolesResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.UserAdministrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class UserAdministrationController {

    private final UserAdministrationService userAdministrationService;

    public UserAdministrationController(UserAdministrationService userAdministrationService) {
        this.userAdministrationService = userAdministrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userAdministrationService.createUser(request);
    }

    @PatchMapping("/{appUserId}/activate")
    public AccountStatusResponse activate(@PathVariable Long appUserId) {
        return userAdministrationService.activate(appUserId);
    }

    @PatchMapping("/{appUserId}/deactivate")
    public AccountStatusResponse deactivate(@PathVariable Long appUserId) {
        return userAdministrationService.deactivate(appUserId);
    }

    @PatchMapping("/{appUserId}/roles")
    public UserRolesResponse updateRoles(
            @PathVariable Long appUserId,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        return userAdministrationService.updateRoles(appUserId, request);
    }
}

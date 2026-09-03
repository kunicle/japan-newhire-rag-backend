package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.NewHireProvisioningRequest;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.NewHireProvisioningResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.UserAdministrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hr/new-hires")
@PreAuthorize("hasRole('HR_MANAGER')")
public class HrNewHireProvisioningController {

    private final UserAdministrationService userAdministrationService;

    public HrNewHireProvisioningController(
            UserAdministrationService userAdministrationService
    ) {
        this.userAdministrationService = userAdministrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NewHireProvisioningResponse provision(
            @Valid @RequestBody NewHireProvisioningRequest request
    ) {
        return userAdministrationService.provisionNewHire(request);
    }
}

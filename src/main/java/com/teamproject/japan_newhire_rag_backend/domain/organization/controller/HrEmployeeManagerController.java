package com.teamproject.japan_newhire_rag_backend.domain.organization.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.ChangeDirectManagerRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.DirectManagerResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.DirectManagerCommandService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hr/employees")
@PreAuthorize("hasRole('HR_MANAGER')")
public class HrEmployeeManagerController {

    private final DirectManagerCommandService directManagerCommandService;

    public HrEmployeeManagerController(DirectManagerCommandService directManagerCommandService) {
        this.directManagerCommandService = directManagerCommandService;
    }

    @PatchMapping("/{employeeId}/manager")
    public DirectManagerResponse changeDirectManager(
            @PathVariable Long employeeId,
            @Valid @RequestBody ChangeDirectManagerRequest request
    ) {
        return directManagerCommandService.changeDirectManager(employeeId, request);
    }
}

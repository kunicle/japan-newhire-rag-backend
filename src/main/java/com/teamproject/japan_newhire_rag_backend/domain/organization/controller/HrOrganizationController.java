package com.teamproject.japan_newhire_rag_backend.domain.organization.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.ChangeEmployeeOrganizationRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.CreateDepartmentRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.UpdateDepartmentRequest;
import com.teamproject.japan_newhire_rag_backend.domain.organization.controller.dto.OrganizationDepartmentResponse;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.EmployeeOrganizationCommandService;
import com.teamproject.japan_newhire_rag_backend.domain.organization.service.internal.DepartmentCommandService;

@RestController
@RequestMapping("/api/hr")
@PreAuthorize("hasRole('HR_MANAGER')")
public class HrOrganizationController {
    private final EmployeeOrganizationCommandService employees;
    private final DepartmentCommandService departments;

    public HrOrganizationController(EmployeeOrganizationCommandService employees,
            DepartmentCommandService departments) {
        this.employees = employees;
        this.departments = departments;
    }

    @PatchMapping("/employees/{employeeId}/organization")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOrganization(@PathVariable Long employeeId,
            @Valid @RequestBody ChangeEmployeeOrganizationRequest request) {
        employees.changeOrganization(employeeId, request);
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return departments.create(request);
    }

    @PatchMapping("/departments/{departmentId}")
    public OrganizationDepartmentResponse updateDepartment(@PathVariable Long departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return departments.update(departmentId, request);
    }
}

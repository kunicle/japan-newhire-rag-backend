package com.teamproject.japan_newhire_rag_backend.domain.organization.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum OrganizationErrorCode implements ErrorCodeSpec {

    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Department not found"),
    JOB_GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "Active job grade not found"),
    MANAGER_GRADE_NOT_ALLOWED(HttpStatus.CONFLICT, "A manager cannot have a lower job grade than their employee"),
    MANAGER_CYCLE_NOT_ALLOWED(HttpStatus.CONFLICT, "Manager relation would create a cycle"),
    DEPARTMENT_CYCLE_NOT_ALLOWED(HttpStatus.CONFLICT, "Department hierarchy would create a cycle"),
    DEPARTMENT_CODE_CONFLICT(HttpStatus.CONFLICT, "Department code already exists"),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "Employee not found"),
    MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "Manager employee not found"),
    SELF_MANAGER_NOT_ALLOWED(HttpStatus.CONFLICT, "An employee cannot be their own manager"),
    MANAGER_RELATION_DATA_CONFLICT(
            HttpStatus.CONFLICT,
            "Manager relation data is inconsistent"),

    ORGANIZATION_DATA_CONFLICT(
            HttpStatus.CONFLICT,
            "Organization data is inconsistent");

    private final HttpStatus status;
    private final String defaultMessage;

    OrganizationErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}

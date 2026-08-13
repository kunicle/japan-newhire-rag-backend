package com.teamproject.japan_newhire_rag_backend.domain.organization.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum OrganizationErrorCode implements ErrorCodeSpec {

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

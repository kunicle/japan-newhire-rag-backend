package com.teamproject.japan_newhire_rag_backend.domain.auth.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum UserAdministrationErrorCode implements ErrorCodeSpec {
    APP_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "App user not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    EMPLOYEE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Employee number already exists"),
    DEPARTMENT_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Department does not exist or is not active"),
    JOB_GRADE_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Job grade does not exist or is not active"),
    INVALID_ACCOUNT_STATUS_TRANSITION(HttpStatus.CONFLICT, "Account status transition is not allowed"),
    ROLE_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Role does not exist or is not active"),
    USER_DATA_CONFLICT(HttpStatus.CONFLICT, "User data is inconsistent");

    private final HttpStatus status;
    private final String defaultMessage;

    UserAdministrationErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    @Override public HttpStatus status() { return status; }
    @Override public String code() { return name(); }
    @Override public String defaultMessage() { return defaultMessage; }
}

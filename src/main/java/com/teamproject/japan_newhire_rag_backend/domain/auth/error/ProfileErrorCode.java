package com.teamproject.japan_newhire_rag_backend.domain.auth.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum ProfileErrorCode implements ErrorCodeSpec {

    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Profile not found"),
    PROFILE_DATA_CONFLICT(HttpStatus.CONFLICT, "Profile data is inconsistent");

    private final HttpStatus status;
    private final String defaultMessage;

    ProfileErrorCode(HttpStatus status, String defaultMessage) {
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

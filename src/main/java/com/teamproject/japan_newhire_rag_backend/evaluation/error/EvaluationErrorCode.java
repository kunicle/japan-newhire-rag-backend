package com.teamproject.japan_newhire_rag_backend.evaluation.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum EvaluationErrorCode implements ErrorCodeSpec {

    EVALUATION_CYCLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Evaluation cycle not found"),
    EVALUATION_CYCLE_INVALID_DATE(HttpStatus.BAD_REQUEST, "Evaluation cycle dates are invalid"),
    EVALUATION_CYCLE_NOT_EDITABLE(HttpStatus.CONFLICT, "Evaluation cycle is not editable"),
    EVALUATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Evaluation operation access is denied");

    private final HttpStatus status;
    private final String defaultMessage;

    EvaluationErrorCode(HttpStatus status, String defaultMessage) {
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

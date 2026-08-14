package com.teamproject.japan_newhire_rag_backend.evaluation.error;

import org.springframework.http.HttpStatus;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public enum EvaluationErrorCode implements ErrorCodeSpec {

    EVALUATION_CYCLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Evaluation cycle not found"),
    EVALUATION_CYCLE_INVALID_DATE(HttpStatus.BAD_REQUEST, "Evaluation cycle dates are invalid"),
    EVALUATION_CYCLE_NOT_EDITABLE(HttpStatus.CONFLICT, "Evaluation cycle is not editable"),
    EVALUATION_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Evaluation template not found"),
    EVALUATION_TEMPLATE_INVALID_VALUE(HttpStatus.BAD_REQUEST, "Evaluation template values are invalid"),
    EVALUATION_TEMPLATE_DUPLICATE_TYPE(HttpStatus.CONFLICT, "Evaluation template type already exists in cycle"),
    EVALUATION_TEMPLATE_NOT_EDITABLE(HttpStatus.CONFLICT, "Evaluation template is not editable"),
    EVALUATION_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Evaluation item not found"),
    EVALUATION_ITEM_INVALID_VALUE(HttpStatus.BAD_REQUEST, "Evaluation item values are invalid"),
    EVALUATION_ITEM_DUPLICATE_ORDER(HttpStatus.CONFLICT, "Evaluation item order already exists in template"),
    EVALUATION_ITEM_NOT_EDITABLE(HttpStatus.CONFLICT, "Evaluation item is not editable"),
    EVALUATION_ASSIGNMENT_INVALID_VALUE(HttpStatus.BAD_REQUEST, "Evaluation assignment values are invalid"),
    EVALUATION_DUPLICATE_ASSIGNMENT(HttpStatus.CONFLICT, "Evaluation assignment already exists"),
    EVALUATION_TARGET_INVALID(HttpStatus.BAD_REQUEST, "Evaluation target employee is invalid"),
    EVALUATION_EVALUATOR_INVALID(HttpStatus.BAD_REQUEST, "Evaluation evaluator employee is invalid"),
    EVALUATION_MANAGER_RELATION_INVALID(HttpStatus.CONFLICT, "Evaluation direct manager relation is invalid"),
    EVALUATION_TEMPLATE_NOT_READY(HttpStatus.CONFLICT, "Evaluation templates are not ready"),
    EVALUATION_CYCLE_NOT_ASSIGNABLE(HttpStatus.CONFLICT, "Evaluation cycle is not assignable"),
    EVALUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Evaluation not found"),
    EVALUATION_NOT_WRITABLE(HttpStatus.CONFLICT, "Evaluation is not writable"),
    EVALUATION_NOT_OWNER(HttpStatus.FORBIDDEN, "Evaluation is not owned by current employee"),
    EVALUATION_ITEM_MISMATCH(HttpStatus.BAD_REQUEST, "Evaluation item does not belong to evaluation template"),
    EVALUATION_SCORE_INVALID(HttpStatus.BAD_REQUEST, "Evaluation score is invalid"),
    EVALUATION_FEEDBACK_INVALID(HttpStatus.BAD_REQUEST, "Evaluation feedback is invalid"),
    EVALUATION_FEEDBACK_CONFLICT(HttpStatus.CONFLICT, "Evaluation feedback data is conflicting"),
    EVALUATION_NOT_PUBLISHABLE(HttpStatus.CONFLICT, "Evaluation is not publishable"),
    EVALUATION_PUBLISH_CONFLICT(HttpStatus.CONFLICT, "Evaluation publish data is conflicting"),
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

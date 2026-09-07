package com.teamproject.japan_newhire_rag_backend.common.exception;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;

public class BusinessException extends RuntimeException {

    private final ErrorCodeSpec errorCode;

    public BusinessException(ErrorCodeSpec errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ErrorCodeSpec errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCodeSpec getErrorCode() {
        return errorCode;
    }
}

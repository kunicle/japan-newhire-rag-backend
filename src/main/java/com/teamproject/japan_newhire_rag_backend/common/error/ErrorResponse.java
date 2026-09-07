package com.teamproject.japan_newhire_rag_backend.common.error;

public record ErrorResponse(
        int status,
        String code,
        String message
) {

    public static ErrorResponse from(ErrorCodeSpec errorCode) {
        return from(errorCode, errorCode.defaultMessage());
    }

    public static ErrorResponse from(ErrorCodeSpec errorCode, String message) {
        return new ErrorResponse(
                errorCode.status().value(),
                errorCode.code(),
                message);
    }
}

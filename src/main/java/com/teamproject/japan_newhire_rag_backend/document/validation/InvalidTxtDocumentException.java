package com.teamproject.japan_newhire_rag_backend.document.validation;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;

public class InvalidTxtDocumentException extends BusinessException {

    public InvalidTxtDocumentException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}

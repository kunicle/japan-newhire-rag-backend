package com.teamproject.japan_newhire_rag_backend.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCodeSpec {

    HttpStatus status();

    String code();

    String defaultMessage();
}

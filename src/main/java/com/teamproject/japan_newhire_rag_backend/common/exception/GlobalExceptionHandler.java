package com.teamproject.japan_newhire_rag_backend.common.exception;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCodeSpec;
import com.teamproject.japan_newhire_rag_backend.common.error.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception
    ) {
        return errorResponse(
                ErrorCode.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED.defaultMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorCodeSpec errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.from(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        return validationError(exception.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException exception) {
        return validationError(exception.getBindingResult());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        return errorResponse(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.defaultMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.defaultMessage());
        return errorResponse(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        String message = Optional.ofNullable(exception.getMessage())
                .filter(value -> !value.isBlank())
                .orElse(ErrorCode.INVALID_REQUEST.defaultMessage());
        return errorResponse(ErrorCode.INVALID_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        return errorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage());
    }

    private ResponseEntity<ErrorResponse> validationError(BindingResult bindingResult) {
        String message = bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(this::fieldErrorMessage)
                .orElse(ErrorCode.VALIDATION_ERROR.defaultMessage());
        return errorResponse(ErrorCode.VALIDATION_ERROR, message);
    }

    private String fieldErrorMessage(FieldError fieldError) {
        String detail = Optional.ofNullable(fieldError.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.defaultMessage());
        return fieldError.getField() + ": " + detail;
    }

    private ResponseEntity<ErrorResponse> errorResponse(ErrorCodeSpec errorCode, String message) {
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.from(errorCode, message));
    }
}

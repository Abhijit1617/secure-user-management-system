package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of the application's exception hierarchy. Every subclass carries the
 * {@link HttpStatus} it should be translated to, so
 * {@link GlobalExceptionHandler} never has to guess which status code a
 * given business failure maps to.
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected BusinessException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

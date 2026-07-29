package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised for business-rule violations that Bean Validation cannot express
 * because they depend on cross-referencing other data (e.g. "a department
 * cannot be its own parent"), as opposed to a malformed request shape.
 */
public class InvalidRequestException extends BusinessException {

    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
    }
}

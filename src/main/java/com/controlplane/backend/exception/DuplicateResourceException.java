package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }

    public static DuplicateResourceException of(String resourceName, String fieldName, Object fieldValue) {
        return new DuplicateResourceException(
                "%s already exists with %s: %s".formatted(resourceName, fieldName, fieldValue));
    }
}

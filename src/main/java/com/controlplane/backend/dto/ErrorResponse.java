package com.controlplane.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error payload returned by every failure path in the API,
 * whether raised inside the security filter chain or the global exception
 * handler, so clients only ever have to parse one error shape.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private int status;

    private String error;

    private String message;

    private String path;

    /**
     * Populated only for Bean Validation failures: field name mapped to the
     * corresponding violation message.
     */
    private Map<String, String> validationErrors;
}

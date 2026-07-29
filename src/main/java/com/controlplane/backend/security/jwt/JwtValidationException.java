package com.controlplane.backend.security.jwt;

/**
 * Raised internally by {@link JwtTokenProvider} when a token fails
 * signature, structure or expiration checks. Never escapes the security
 * filter chain — {@link JwtAuthenticationFilter} catches it and converts
 * the message into the {@code jwt_error} request attribute that
 * {@code CustomAuthenticationEntryPoint} reads back out.
 */
public class JwtValidationException extends RuntimeException {

    public JwtValidationException(String message) {
        super(message);
    }

    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

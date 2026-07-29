package com.controlplane.backend.security;

/**
 * Central home for magic strings used across the security package so that
 * the header name, token prefix and claim keys are defined exactly once.
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Request attribute key used by the JWT filter to pass a human readable
     * failure reason (expired, malformed, unsupported, bad signature) to
     * {@link CustomAuthenticationEntryPoint} without exposing raw exception
     * internals to the client.
     */
    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TOKEN_TYPE = "type";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    public static final String ROLE_PREFIX = "ROLE_";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info"
    };
}

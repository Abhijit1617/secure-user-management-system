package com.controlplane.backend.security.jwt;

import java.util.List;
import java.util.UUID;

/**
 * Plain carrier for the claims {@link JwtTokenProvider} extracts out of an
 * access token, so callers such as {@link JwtAuthenticationFilter} don't
 * have to work with the raw {@code io.jsonwebtoken.Claims} map.
 */
public record JwtClaims(
        UUID userId,
        String username,
        List<String> roles,
        List<String> permissions
) {
}

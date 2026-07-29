package com.controlplane.backend.security.jwt;

import com.controlplane.backend.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Small, dependency-free helpers used by the security filter to pull a
 * bearer token out of an incoming request. Kept separate from
 * {@link JwtTokenProvider} because it has nothing to do with signing or
 * parsing — it is pure HTTP plumbing.
 */
public final class JwtUtility {

    private JwtUtility() {
    }

    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}

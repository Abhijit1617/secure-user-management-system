package com.controlplane.backend.security.jwt;

import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request, before {@code UsernamePasswordAuthenticationFilter},
 * and is the only place in the application that reads a raw bearer token.
 * A missing token is not an error here — the request simply proceeds
 * unauthenticated and {@code SecurityConfig}'s authorization rules decide
 * whether that is acceptable for the endpoint being called.
 * <p>
 * A present but invalid token is also allowed to proceed unauthenticated
 * rather than short-circuiting the filter chain: this lets
 * {@code CustomAuthenticationEntryPoint} produce a consistent 401 body for
 * both "no token" and "bad token" cases, using the reason left in the
 * {@code jwt_error} request attribute.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String token = JwtUtility.resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                JwtClaims claims = jwtService.parseAccessToken(token);
                CustomUserDetails principal = CustomUserDetails.fromClaims(claims);

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtValidationException exception) {
                log.debug("Rejected bearer token on {}: {}", request.getRequestURI(), exception.getMessage());
                SecurityContextHolder.clearContext();
                request.setAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE, exception.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}

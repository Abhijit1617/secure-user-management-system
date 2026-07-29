package com.controlplane.backend.security;

import com.controlplane.backend.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Invoked by Spring Security whenever an unauthenticated request reaches a
 * protected endpoint, or the {@link JwtAuthenticationFilter} rejects a
 * bearer token. Returns a JSON body matching the rest of the API's error
 * shape instead of the framework's default HTML/plain-text response.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                          HttpServletResponse response,
                          AuthenticationException authException) throws IOException {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(resolveMessage(request))
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String resolveMessage(HttpServletRequest request) {
        Object jwtError = request.getAttribute(SecurityConstants.JWT_ERROR_ATTRIBUTE);
        if (jwtError instanceof String message && !message.isBlank()) {
            return message;
        }
        return "Authentication is required to access this resource";
    }
}

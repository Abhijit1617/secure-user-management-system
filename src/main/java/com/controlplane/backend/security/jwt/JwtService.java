package com.controlplane.backend.security.jwt;

import com.controlplane.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The facade the rest of the application talks to for anything JWT
 * related. {@link JwtTokenProvider} owns the actual cryptographic work;
 * this class translates that into the shapes the authentication service
 * and controllers need (token strings, expiry in seconds for API
 * responses).
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;

    public String generateAccessToken(CustomUserDetails userDetails) {
        return jwtTokenProvider.generateAccessToken(userDetails);
    }

    public String generateRefreshTokenValue() {
        return jwtTokenProvider.generateRefreshTokenValue();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtTokenProvider.getAccessTokenExpirationMs() / 1000;
    }

    public long getRefreshTokenExpirationMs() {
        return jwtTokenProvider.getRefreshTokenExpirationMs();
    }

    public JwtClaims parseAccessToken(String token) {
        return jwtTokenProvider.validateAndParseAccessToken(token);
    }
}

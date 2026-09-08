package com.controlplane.backend.security.jwt;

import com.controlplane.backend.config.JwtProperties;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.security.SecurityConstants;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Owns every low-level JWT operation: signing new access tokens, parsing
 * and validating incoming ones, and generating the opaque random value
 * used for refresh tokens. Nothing above this class ever touches the jjwt
 * API directly.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    /**
     * Builds a signed access token carrying the user's id as subject and
     * their roles/permissions flattened into claims, per
     * {@code docs/security-architecture.md}.
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpirationMs());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(SecurityConstants.ROLE_PREFIX))
                .map(authority -> authority.substring(SecurityConstants.ROLE_PREFIX.length()))
                .collect(Collectors.toList());

        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith(SecurityConstants.ROLE_PREFIX))
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userDetails.getId().toString())
                .claim("username", userDetails.getUsername())
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_PERMISSIONS, permissions)
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, SecurityConstants.TOKEN_TYPE_ACCESS)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a cryptographically random, URL-safe opaque value for a
     * refresh token. Refresh tokens are intentionally not JWTs — they carry
     * no claims and their validity is looked up in {@code refresh_tokens},
     * which is what makes server-side revocation possible.
     */
    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpirationMs();
    }

    /**
     * Validates signature, issuer and expiration, and confirms the token is
     * actually an access token rather than a refresh token or a token
     * issued for another purpose. Throws {@link JwtValidationException}
     * with a message safe to surface to the client on any failure.
     */
    public JwtClaims validateAndParseAccessToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
            if (!SecurityConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                throw new JwtValidationException("Token is not a valid access token");
            }

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(SecurityConstants.CLAIM_ROLES, List.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get(SecurityConstants.CLAIM_PERMISSIONS, List.class);

            return new JwtClaims(
                    UUID.fromString(claims.getSubject()),
                    claims.get("username", String.class),
                    roles,
                    permissions
            );
        } catch (ExpiredJwtException exception) {
            throw new JwtValidationException("Access token has expired", exception);
        } catch (SignatureException exception) {
            log.warn("Rejected JWT with invalid signature");
            throw new JwtValidationException("Access token signature is invalid", exception);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtValidationException("Access token is malformed or invalid", exception);
        }
    }
}

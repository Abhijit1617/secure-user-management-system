package com.controlplane.backend.service.impl;

import com.controlplane.backend.entity.RefreshToken;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.exception.InvalidTokenException;
import com.controlplane.backend.repository.RefreshTokenRepository;
import com.controlplane.backend.security.jwt.JwtService;
import com.controlplane.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implements the one-time-use refresh token rotation flow documented in
 * {@code docs/security-architecture.md}. A token that is presented after
 * it has already been rotated is treated as a replay signal: every active
 * token for that user is revoked, forcing re-authentication everywhere.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RefreshToken issue(User user, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtService.generateRefreshTokenValue())
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken rotate(String presentedToken, String deviceInfo, String ipAddress) {
        RefreshToken existing = refreshTokenRepository.findByToken(presentedToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (existing.isRevoked()) {
            log.warn("Detected reuse of a revoked refresh token for user {}. Revoking all sessions.",
                    existing.getUser().getId());
            refreshTokenRepository.revokeAllForUser(existing.getUser());
            throw new InvalidTokenException(
                    "Refresh token has already been used. All sessions have been revoked for your security");
        }

        if (existing.isExpired()) {
            throw new InvalidTokenException("Refresh token has expired, please log in again");
        }

        RefreshToken newToken = issue(existing.getUser(), deviceInfo, ipAddress);

        existing.setRevoked(true);
        existing.setReplacedByToken(newToken.getToken());
        refreshTokenRepository.save(existing);

        return newToken;
    }

    @Override
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
}

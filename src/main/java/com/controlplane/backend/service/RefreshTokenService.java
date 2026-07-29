package com.controlplane.backend.service;

import com.controlplane.backend.entity.RefreshToken;
import com.controlplane.backend.entity.User;

public interface RefreshTokenService {

    RefreshToken issue(User user, String deviceInfo, String ipAddress);

    /**
     * Validates the presented token and atomically rotates it: the old
     * token is revoked and a brand new one is issued and returned.
     */
    RefreshToken rotate(String presentedToken, String deviceInfo, String ipAddress);

    void revokeAllForUser(User user);

    void revokeToken(String token);
}

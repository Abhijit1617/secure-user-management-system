package com.controlplane.backend.service;

import com.controlplane.backend.dto.auth.AuthenticationResponse;
import com.controlplane.backend.dto.auth.ChangePasswordRequest;
import com.controlplane.backend.dto.auth.CurrentUserResponse;
import com.controlplane.backend.dto.auth.ForgotPasswordRequest;
import com.controlplane.backend.dto.auth.LoginRequest;
import com.controlplane.backend.dto.auth.MessageResponse;
import com.controlplane.backend.dto.auth.RefreshTokenResponse;
import com.controlplane.backend.dto.auth.RegisterRequest;
import com.controlplane.backend.dto.auth.ResendVerificationRequest;
import com.controlplane.backend.dto.auth.ResetPasswordRequest;

import java.util.UUID;

public interface AuthService {

    MessageResponse register(RegisterRequest request);

    AuthenticationResponse login(LoginRequest request, String deviceInfo, String ipAddress);

    void logout(String refreshToken);

    RefreshTokenResponse refresh(String refreshToken, String deviceInfo, String ipAddress);

    CurrentUserResponse getCurrentUser(UUID userId);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(String token);

    MessageResponse resendVerificationEmail(ResendVerificationRequest request);

    void deactivateAccount(UUID userId);

    void reactivateAccount(UUID userId);
}

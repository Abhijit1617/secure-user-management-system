package com.controlplane.backend.controller;

import com.controlplane.backend.dto.auth.AuthenticationResponse;
import com.controlplane.backend.dto.auth.ChangePasswordRequest;
import com.controlplane.backend.dto.auth.CurrentUserResponse;
import com.controlplane.backend.dto.auth.ForgotPasswordRequest;
import com.controlplane.backend.dto.auth.LoginRequest;
import com.controlplane.backend.dto.auth.MessageResponse;
import com.controlplane.backend.dto.auth.RefreshTokenRequest;
import com.controlplane.backend.dto.auth.RefreshTokenResponse;
import com.controlplane.backend.dto.auth.RegisterRequest;
import com.controlplane.backend.dto.auth.ResendVerificationRequest;
import com.controlplane.backend.dto.auth.ResetPasswordRequest;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh and account lifecycle")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account",
            description = "Creates an account with the default EMPLOYEE role and sends a verification email. "
                    + "The account cannot log in until the email is verified.")
    @ApiResponse(responseCode = "201", description = "Registration accepted, verification email sent")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with username/email and password",
            description = "Returns a short-lived access token and a rotating refresh token.")
    @ApiResponse(responseCode = "200", description = "Authenticated successfully")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "403", description = "Account not verified or deactivated")
    @ApiResponse(responseCode = "423", description = "Account locked due to repeated failed attempts")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request,
                                                          HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, deviceInfo, ipAddress));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access/refresh token pair",
            description = "Refresh tokens are one-time use. Presenting an already-used token revokes every "
                    + "active session for that user as a replay-attack safeguard.")
    @ApiResponse(responseCode = "200", description = "Token pair rotated successfully")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired or already used")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                          HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken(), deviceInfo, ipAddress));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token", description = "Ends the session tied to the given refresh token.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Current user returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change the authenticated user's password",
            description = "Revokes every active refresh token for the account, requiring re-login on other devices.")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Current password incorrect or new password too weak")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email",
            description = "Always returns 202 regardless of whether the email is registered, to prevent "
                    + "account enumeration.")
    @ApiResponse(responseCode = "202", description = "Request accepted")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().body(
                MessageResponse.of("If an account exists for that email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password using a token from the forgot-password email")
    @ApiResponse(responseCode = "204", description = "Password reset successfully")
    @ApiResponse(responseCode = "401", description = "Reset token invalid or expired")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify an email address using the token from the verification email")
    @ApiResponse(responseCode = "204", description = "Email verified")
    @ApiResponse(responseCode = "401", description = "Verification token invalid or expired")
    public ResponseEntity<Void> verifyEmail(@NotBlank @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend the email verification link")
    @ApiResponse(responseCode = "200", description = "Request processed")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerificationEmail(request));
    }

    @DeleteMapping("/deactivate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate the authenticated user's own account")
    @ApiResponse(responseCode = "204", description = "Account deactivated")
    public ResponseEntity<Void> deactivateAccount(@AuthenticationPrincipal CustomUserDetails principal) {
        authService.deactivateAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reactivate/{userId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reactivate a deactivated account (administrator only)")
    @ApiResponse(responseCode = "204", description = "Account reactivated")
    @ApiResponse(responseCode = "403", description = "Missing USER_UPDATE permission")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Void> reactivateAccount(@PathVariable UUID userId) {
        authService.reactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }
}

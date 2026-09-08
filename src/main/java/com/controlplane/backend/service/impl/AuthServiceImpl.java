package com.controlplane.backend.service.impl;

import com.controlplane.backend.config.SecurityProperties;
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
import com.controlplane.backend.entity.RefreshToken;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.entity.enums.AuditAction;
import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.exception.AccountDeactivatedException;
import com.controlplane.backend.exception.AccountLockedException;
import com.controlplane.backend.exception.AccountNotVerifiedException;
import com.controlplane.backend.exception.InvalidCredentialsException;
import com.controlplane.backend.exception.InvalidPasswordException;
import com.controlplane.backend.exception.InvalidTokenException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.UserMapper;
import com.controlplane.backend.repository.RoleRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.security.jwt.JwtService;
import com.controlplane.backend.service.AuthService;
import com.controlplane.backend.service.EmailService;
import com.controlplane.backend.service.EventPublisherService;
import com.controlplane.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String DEFAULT_ROLE_NAME = "EMPLOYEE";
    private static final String VERIFICATION_KEY_PREFIX = "email-verification:";
    private static final String RESET_KEY_PREFIX = "password-reset:";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties securityProperties;
    private final UserMapper userMapper;
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role " + DEFAULT_ROLE_NAME + " is not seeded in the database"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();
        user.addRole(defaultRole);
        user = userRepository.save(user);

        issueVerificationToken(user);

        eventPublisherService.publishAuditEvent(
                AuditEvent.of(user.getId(), AuditAction.CREATE, "User", user.getId(), null, null));

        log.info("Registered new user account [{}]", user.getUsername());
        return MessageResponse.of(
                "Registration successful. Please check your email to verify your account before logging in.");
    }

    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(InvalidCredentialsException::new);

        unlockIfLockExpired(user);

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException(user.getAccountLockedUntil());
        }
        if (user.getStatus() == UserStatus.DEACTIVATED || user.getStatus() == UserStatus.INACTIVE) {
            throw new AccountDeactivatedException();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            registerFailedLoginAndThrow(user, ipAddress);
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION || !user.isEmailVerified()) {
            throw new AccountNotVerifiedException();
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        CustomUserDetails principal = CustomUserDetails.fromUser(user);
        String accessToken = jwtService.generateAccessToken(principal);
        RefreshToken refreshToken = refreshTokenService.issue(user, deviceInfo, ipAddress);

        eventPublisherService.publishAuditEvent(
                AuditEvent.of(user.getId(), AuditAction.LOGIN, "User", user.getId(), ipAddress, deviceInfo));

        log.info("User [{}] authenticated successfully", user.getUsername());
        return buildAuthenticationResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    @Override
    @Transactional
    public RefreshTokenResponse refresh(String refreshToken, String deviceInfo, String ipAddress) {
        RefreshToken rotated = refreshTokenService.rotate(refreshToken, deviceInfo, ipAddress);
        User user = rotated.getUser();
        CustomUserDetails principal = CustomUserDetails.fromUser(user);
        String accessToken = jwtService.generateAccessToken(principal);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rotated.getToken())
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID userId) {
        User user = loadActiveUser(userId);
        return userMapper.toCurrentUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = loadActiveUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        log.info("User [{}] changed their password", user.getUsername());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .filter(user -> !user.isDeleted())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    redisTemplate.opsForValue().set(
                            RESET_KEY_PREFIX + resetToken,
                            user.getId().toString(),
                            Duration.ofMinutes(securityProperties.getPasswordResetTokenExpirationMinutes()));
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
                });
        // Deliberately no branch on "user not found" — responding identically
        // either way prevents email enumeration through this endpoint.
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String key = RESET_KEY_PREFIX + request.getToken();
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new InvalidTokenException("Reset token is invalid or has expired");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new InvalidTokenException("Reset token is invalid or has expired"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        redisTemplate.delete(key);

        log.info("User [{}] completed a password reset", user.getUsername());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String key = VERIFICATION_KEY_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new InvalidTokenException("Verification link is invalid or has expired");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new InvalidTokenException("Verification link is invalid or has expired"));

        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        redisTemplate.delete(key);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        log.info("User [{}] verified their email address", user.getUsername());
    }

    @Override
    @Transactional
    public MessageResponse resendVerificationEmail(ResendVerificationRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> !user.isDeleted())
                .map(user -> {
                    if (user.isEmailVerified()) {
                        return MessageResponse.of("This email address is already verified.");
                    }
                    issueVerificationToken(user);
                    return MessageResponse.of("Verification email sent. Please check your inbox.");
                })
                .orElse(MessageResponse.of(
                        "If an account exists for that email, a verification link has been sent."));
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = loadActiveUser(userId);
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        log.info("User [{}] deactivated their account", user.getUsername());
    }

    @Override
    @Transactional
    public void reactivateAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", userId));
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        log.info("User [{}] was reactivated by an administrator", user.getUsername());
    }

    private void issueVerificationToken(User user) {
        String verificationToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                VERIFICATION_KEY_PREFIX + verificationToken,
                user.getId().toString(),
                Duration.ofHours(securityProperties.getEmailVerificationTokenExpirationHours()));
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
    }

    private void unlockIfLockExpired(User user) {
        if (user.getStatus() == UserStatus.LOCKED
                && user.getAccountLockedUntil() != null
                && Instant.now().isAfter(user.getAccountLockedUntil())) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
        }
    }

    private void registerFailedLoginAndThrow(User user, String ipAddress) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        eventPublisherService.publishAuditEvent(
                AuditEvent.of(user.getId(), AuditAction.LOGIN_FAILED, "User", user.getId(), ipAddress, null));

        if (attempts >= securityProperties.getMaxFailedLoginAttempts()) {
            Instant lockedUntil = Instant.now().plusSeconds(securityProperties.getAccountLockDurationMinutes() * 60L);
            user.setStatus(UserStatus.LOCKED);
            user.setAccountLockedUntil(lockedUntil);
            userRepository.save(user);
            emailService.sendAccountLockedNotice(user.getEmail(), user.getFirstName());
            throw new AccountLockedException(lockedUntil);
        }

        userRepository.save(user);
        throw new InvalidCredentialsException();
    }

    private User loadActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", userId));
    }

    private AuthenticationResponse buildAuthenticationResponse(User user, String accessToken, String refreshToken) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        return AuthenticationResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .roles(roleNames)
                .build();
    }
}

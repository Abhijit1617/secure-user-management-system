package com.controlplane.backend.service.impl;

import com.controlplane.backend.config.SecurityProperties;
import com.controlplane.backend.dto.auth.LoginRequest;
import com.controlplane.backend.dto.auth.MessageResponse;
import com.controlplane.backend.dto.auth.RegisterRequest;
import com.controlplane.backend.entity.RefreshToken;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.exception.AccountLockedException;
import com.controlplane.backend.exception.AccountNotVerifiedException;
import com.controlplane.backend.exception.InvalidCredentialsException;
import com.controlplane.backend.mapper.UserMapper;
import com.controlplane.backend.repository.RoleRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.security.jwt.JwtService;
import com.controlplane.backend.service.EmailService;
import com.controlplane.backend.service.EventPublisherService;
import com.controlplane.backend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SecurityProperties securityProperties;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        lenient().when(securityProperties.getMaxFailedLoginAttempts()).thenReturn(5);
        lenient().when(securityProperties.getAccountLockDurationMinutes()).thenReturn(30);
        lenient().when(securityProperties.getPasswordResetTokenExpirationMinutes()).thenReturn(30);
        lenient().when(securityProperties.getEmailVerificationTokenExpirationHours()).thenReturn(24);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Role role = Role.builder().name("EMPLOYEE").hierarchyLevel(40).build();
        activeUser = User.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .password("hashed-password")
                .firstName("Jane")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
        activeUser.setId(UUID.randomUUID());
        activeUser.addRole(role);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane.doe")
                .password("SecurePass123!")
                .build();

        when(userRepository.findByUsernameOrEmail("jane.doe")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(any(User.class), anyString(), anyString()))
                .thenReturn(RefreshToken.builder().token("refresh-token").build());

        var response = authService.login(request, "JUnit-Agent", "127.0.0.1");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRoles()).containsExactly("EMPLOYEE");
        verify(userRepository, times(1)).save(activeUser);
    }

    @Test
    void loginFailsWithWrongPasswordAndIncrementsFailedAttempts() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane.doe")
                .password("wrong-password")
                .build();

        when(userRepository.findByUsernameOrEmail("jane.doe")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "JUnit-Agent", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void loginLocksAccountAfterMaxFailedAttempts() {
        activeUser.setFailedLoginAttempts(4);
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane.doe")
                .password("wrong-password")
                .build();

        when(userRepository.findByUsernameOrEmail("jane.doe")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "JUnit-Agent", "127.0.0.1"))
                .isInstanceOf(AccountLockedException.class);

        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(activeUser.getAccountLockedUntil()).isNotNull();
        verify(emailService, times(1)).sendAccountLockedNotice(activeUser.getEmail(), activeUser.getFirstName());
    }

    @Test
    void loginRejectsUnverifiedAccount() {
        activeUser.setStatus(UserStatus.PENDING_VERIFICATION);
        activeUser.setEmailVerified(false);
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane.doe")
                .password("SecurePass123!")
                .build();

        when(userRepository.findByUsernameOrEmail("jane.doe")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, "JUnit-Agent", "127.0.0.1"))
                .isInstanceOf(AccountNotVerifiedException.class);
    }

    @Test
    void registerCreatesUserWithDefaultRoleAndSendsVerificationEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .username("new.user")
                .email("new.user@example.com")
                .password("SecurePass123!")
                .firstName("New")
                .lastName("User")
                .build();

        Role employeeRole = Role.builder().name("EMPLOYEE").hierarchyLevel(40).build();
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(UUID.randomUUID());
            return savedUser;
        });

        MessageResponse response = authService.register(request);

        assertThat(response.getMessage()).containsIgnoringCase("verify");
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
        verify(valueOperations, times(1)).set(anyString(), anyString(), any());
    }

    @Test
    void loginRejectsUnknownUser() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("ghost")
                .password("whatever")
                .build();

        when(userRepository.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "JUnit-Agent", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}

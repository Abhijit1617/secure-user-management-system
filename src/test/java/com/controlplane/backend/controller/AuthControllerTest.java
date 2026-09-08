package com.controlplane.backend.controller;

import com.controlplane.backend.dto.auth.AuthenticationResponse;
import com.controlplane.backend.dto.auth.LoginRequest;
import com.controlplane.backend.dto.auth.MessageResponse;
import com.controlplane.backend.dto.auth.RegisterRequest;
import com.controlplane.backend.exception.InvalidCredentialsException;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.security.jwt.JwtService;
import com.controlplane.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void registerReturnsCreatedWithMessage() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .password("SecurePass123!")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(MessageResponse.of("Registration successful. Please check your email."));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email."));
    }

    @Test
    void registerRejectsWeakPasswordWithValidationError() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .password("weak")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void loginReturnsTokensOnSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane.doe")
                .password("SecurePass123!")
                .build();

        AuthenticationResponse response = AuthenticationResponse.builder()
                .userId(UUID.randomUUID())
                .username("jane.doe")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(900L)
                .roles(List.of("EMPLOYEE"))
                .build();

        when(authService.login(any(LoginRequest.class), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void loginReturnsUnauthorizedForBadCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("jane.doe")
                .password("wrong-password")
                .build();

        when(authService.login(any(LoginRequest.class), any(), any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
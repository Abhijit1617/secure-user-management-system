package com.controlplane.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private UUID userId;
    private String username;
    private String email;

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(example = "6RZ2f1n8...")
    private String refreshToken;

    @Builder.Default
    @Schema(example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Access token lifetime in seconds", example = "900")
    private long expiresIn;

    private List<String> roles;
}

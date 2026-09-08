package com.controlplane.backend.security.jwt;

import com.controlplane.backend.config.JwtProperties;
import com.controlplane.backend.entity.Permission;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        jwtProperties.setSecret(Base64.getEncoder().encodeToString(keyBytes));
        jwtProperties.setAccessTokenExpirationMs(900_000L);
        jwtProperties.setRefreshTokenExpirationMs(604_800_000L);
        jwtProperties.setIssuer("backend-control-plane");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        Permission permission = Permission.builder().name("TASK_CREATE").module("TASK").build();
        Role role = Role.builder().name("MANAGER").hierarchyLevel(60)
                .permissions(Set.of(permission)).build();
        User user = User.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .password("hashed-password")
                .firstName("Jane")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(role))
                .build();
        user.setId(UUID.randomUUID());

        userDetails = CustomUserDetails.fromUser(user);
    }

    @Test
    void generatesAccessTokenThatCanBeParsedBack() {
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        JwtClaims claims = jwtTokenProvider.validateAndParseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(userDetails.getId());
        assertThat(claims.username()).isEqualTo("jane.doe");
        assertThat(claims.roles()).containsExactly("MANAGER");
        assertThat(claims.permissions()).containsExactly("TASK_CREATE");
    }

    @Test
    void rejectsTokenSignedWithADifferentKey() {
        JwtProperties otherProperties = new JwtProperties();
        byte[] otherKeyBytes = new byte[32];
        new SecureRandom().nextBytes(otherKeyBytes);
        otherProperties.setSecret(Base64.getEncoder().encodeToString(otherKeyBytes));
        otherProperties.setAccessTokenExpirationMs(900_000L);
        otherProperties.setRefreshTokenExpirationMs(604_800_000L);
        otherProperties.setIssuer("backend-control-plane");
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);

        String token = otherProvider.generateAccessToken(userDetails);

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseAccessToken(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseAccessToken("not-a-real-token"))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void refreshTokenValuesAreUniqueAndUrlSafe() {
        String first = jwtTokenProvider.generateRefreshTokenValue();
        String second = jwtTokenProvider.generateRefreshTokenValue();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).doesNotContain("+", "/");
    }
}

package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.user.AssignRolesRequest;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.UserMapper;
import com.controlplane.backend.repository.RoleRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .status(UserStatus.ACTIVE)
                .deleted(false)
                .build();
        user.setId(userId);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void softDeleteMarksUserDeletedAndRevokesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.softDelete(userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    void softDeleteThrowsWhenUserAlreadyDeleted() {
        user.setDeleted(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.softDelete(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void restoreClearsDeletedFlagAndReactivatesUser() {
        user.setDeleted(true);
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.restore(userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void assignRolesAddsEachRequestedRole() {
        Role managerRole = Role.builder().name("MANAGER").hierarchyLevel(60).build();
        managerRole.setId(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(managerRole.getId())).thenReturn(Optional.of(managerRole));

        AssignRolesRequest request = AssignRolesRequest.builder().roleIds(Set.of(managerRole.getId())).build();
        userService.assignRoles(userId, request);

        assertThat(user.getRoles()).contains(managerRole);
    }

    @Test
    void getByIdThrowsResourceNotFoundForUnknownUser() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

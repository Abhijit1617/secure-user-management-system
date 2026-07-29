package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.user.AssignRolesRequest;
import com.controlplane.backend.dto.user.CreateUserRequest;
import com.controlplane.backend.dto.user.UpdateProfileRequest;
import com.controlplane.backend.dto.user.UpdateUserRequest;
import com.controlplane.backend.dto.user.UserResponse;
import com.controlplane.backend.entity.enums.UserStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    PageResponse<UserResponse> search(String searchTerm, UserStatus status, boolean includeDeleted,
                                       Pageable pageable);

    UserResponse getById(UUID id);

    UserResponse create(CreateUserRequest request);

    UserResponse update(UUID id, UpdateUserRequest request);

    UserResponse updateOwnProfile(UUID id, UpdateProfileRequest request);

    void softDelete(UUID id);

    void restore(UUID id);

    void enable(UUID id);

    void disable(UUID id);

    UserResponse assignRoles(UUID id, AssignRolesRequest request);

    UserResponse removeRole(UUID id, UUID roleId);
}

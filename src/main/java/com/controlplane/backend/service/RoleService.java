package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.role.AssignPermissionsRequest;
import com.controlplane.backend.dto.role.RoleRequest;
import com.controlplane.backend.dto.role.RoleResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoleService {

    PageResponse<RoleResponse> search(String searchTerm, Pageable pageable);

    RoleResponse getById(UUID id);

    RoleResponse create(RoleRequest request);

    RoleResponse update(UUID id, RoleRequest request);

    void delete(UUID id);

    RoleResponse assignPermissions(UUID id, AssignPermissionsRequest request);

    RoleResponse removePermission(UUID id, UUID permissionId);
}

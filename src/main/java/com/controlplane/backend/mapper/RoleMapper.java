package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.role.RoleResponse;
import com.controlplane.backend.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    public RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .hierarchyLevel(role.getHierarchyLevel())
                .permissions(role.getPermissions().stream()
                        .map(permissionMapper::toResponse)
                        .collect(Collectors.toSet()))
                .createdAt(role.getCreatedAt())
                .build();
    }
}

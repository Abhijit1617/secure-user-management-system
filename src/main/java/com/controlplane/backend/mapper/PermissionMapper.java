package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.permission.PermissionResponse;
import com.controlplane.backend.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permission permission) {
        if (permission == null) {
            return null;
        }
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .module(permission.getModule())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}

package com.controlplane.backend.dto.role;

import com.controlplane.backend.dto.permission.PermissionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    private Integer hierarchyLevel;
    private Set<PermissionResponse> permissions;
    private Instant createdAt;
}

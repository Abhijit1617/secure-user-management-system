package com.controlplane.backend.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
            message = "Role name must be upper snake case, e.g. PROJECT_COORDINATOR")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Hierarchy level is required")
    private Integer hierarchyLevel;

    private Set<UUID> permissionIds;
}

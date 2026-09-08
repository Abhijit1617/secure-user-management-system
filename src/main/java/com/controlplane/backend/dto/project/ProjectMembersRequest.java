package com.controlplane.backend.dto.project;

import jakarta.validation.constraints.NotEmpty;
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
public class ProjectMembersRequest {

    @NotEmpty(message = "At least one employee id must be provided")
    private Set<UUID> employeeIds;
}

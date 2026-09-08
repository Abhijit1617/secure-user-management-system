package com.controlplane.backend.dto.project;

import com.controlplane.backend.entity.enums.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectPriorityRequest {

    @NotNull(message = "Priority is required")
    private Priority priority;
}

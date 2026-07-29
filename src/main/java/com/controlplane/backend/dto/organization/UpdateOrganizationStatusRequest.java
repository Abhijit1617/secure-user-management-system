package com.controlplane.backend.dto.organization;

import com.controlplane.backend.entity.enums.OrganizationStatus;
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
public class UpdateOrganizationStatusRequest {

    @NotNull(message = "Status is required")
    private OrganizationStatus status;
}

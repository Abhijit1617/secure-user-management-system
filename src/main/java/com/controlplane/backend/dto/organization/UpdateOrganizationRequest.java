package com.controlplane.backend.dto.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class UpdateOrganizationRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 200, message = "Legal name must not exceed 200 characters")
    private String legalName;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Valid
    private OrganizationSettingsRequest settings;
}

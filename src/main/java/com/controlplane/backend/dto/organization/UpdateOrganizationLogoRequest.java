package com.controlplane.backend.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sets the organization's logo by URL. Actual image upload is handled by
 * the file management module and is expected to call this endpoint with
 * the resulting storage URL once the upload completes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationLogoRequest {

    @NotBlank(message = "Logo URL is required")
    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;
}

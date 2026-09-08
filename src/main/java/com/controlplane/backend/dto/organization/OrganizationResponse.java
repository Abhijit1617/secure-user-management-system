package com.controlplane.backend.dto.organization;

import com.controlplane.backend.dto.user.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String legalName;
    private String slug;
    private String website;
    private String industry;
    private String logoUrl;
    private String status;
    private UserSummaryResponse owner;
    private OrganizationSettingsResponse settings;
    private int departmentCount;
    private Instant createdAt;
}

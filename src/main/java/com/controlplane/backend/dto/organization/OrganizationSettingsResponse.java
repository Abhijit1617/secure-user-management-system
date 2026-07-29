package com.controlplane.backend.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettingsResponse {

    private String timezone;
    private String defaultLocale;
    private String dateFormat;
    private String currency;
    private boolean allowSelfRegistration;
}

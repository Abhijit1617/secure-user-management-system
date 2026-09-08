package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.organization.OrganizationResponse;
import com.controlplane.backend.dto.organization.OrganizationSettingsResponse;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.OrganizationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationMapper {

    private final UserMapper userMapper;

    public OrganizationResponse toResponse(Organization organization, int departmentCount) {
        if (organization == null) {
            return null;
        }
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .legalName(organization.getLegalName())
                .slug(organization.getSlug())
                .website(organization.getWebsite())
                .industry(organization.getIndustry())
                .logoUrl(organization.getLogoUrl())
                .status(organization.getStatus().name())
                .owner(userMapper.toSummaryResponse(organization.getOwner()))
                .settings(toSettingsResponse(organization.getSettings()))
                .departmentCount(departmentCount)
                .createdAt(organization.getCreatedAt())
                .build();
    }

    private OrganizationSettingsResponse toSettingsResponse(OrganizationSettings settings) {
        if (settings == null) {
            return null;
        }
        return OrganizationSettingsResponse.builder()
                .timezone(settings.getTimezone())
                .defaultLocale(settings.getDefaultLocale())
                .dateFormat(settings.getDateFormat())
                .currency(settings.getCurrency())
                .allowSelfRegistration(settings.isAllowSelfRegistration())
                .build();
    }
}

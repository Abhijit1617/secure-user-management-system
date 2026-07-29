package com.controlplane.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tenant-level configuration embedded directly onto {@link Organization}.
 * Kept as typed columns rather than a JSON blob so that individual settings
 * can be validated, indexed and queried like any other column if a future
 * feature needs to filter organizations by one of them.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettings {

    @Column(name = "settings_timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "settings_default_locale", length = 10)
    @Builder.Default
    private String defaultLocale = "en-US";

    @Column(name = "settings_date_format", length = 20)
    @Builder.Default
    private String dateFormat = "yyyy-MM-dd";

    @Column(name = "settings_currency", length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "settings_allow_self_registration", nullable = false)
    @Builder.Default
    private boolean allowSelfRegistration = false;
}

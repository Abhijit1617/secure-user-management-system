package com.controlplane.backend.dto.organization;

import jakarta.validation.constraints.NotBlank;
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
public class OrganizationSettingsRequest {

    @NotBlank(message = "Timezone is required")
    @Builder.Default
    private String timezone = "UTC";

    @NotBlank(message = "Default locale is required")
    @Builder.Default
    private String defaultLocale = "en-US";

    @NotBlank(message = "Date format is required")
    @Builder.Default
    private String dateFormat = "yyyy-MM-dd";

    @NotBlank(message = "Currency is required")
    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private boolean allowSelfRegistration = false;
}

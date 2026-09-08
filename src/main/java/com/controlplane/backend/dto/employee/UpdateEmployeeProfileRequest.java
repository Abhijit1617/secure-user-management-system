package com.controlplane.backend.dto.employee;

import jakarta.validation.Valid;
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
public class UpdateEmployeeProfileRequest {

    @Valid
    private AddressDto address;

    @Valid
    private EmergencyContactDto emergencyContact;
}

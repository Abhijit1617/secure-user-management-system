package com.controlplane.backend.dto.employee;

import com.controlplane.backend.validation.ValidPhoneNumber;
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
public class EmergencyContactDto {

    @Size(max = 150, message = "Contact name must not exceed 150 characters")
    private String contactName;

    @Size(max = 50, message = "Relationship must not exceed 50 characters")
    private String relationship;

    @ValidPhoneNumber
    private String phoneNumber;

    @ValidPhoneNumber
    private String alternatePhoneNumber;
}

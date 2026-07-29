package com.controlplane.backend.dto.employee;

import com.controlplane.backend.entity.enums.EmploymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {

    @NotNull(message = "Organization id is required")
    private UUID organizationId;

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotBlank(message = "Employee code is required")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Employee code must be uppercase alphanumeric, e.g. EMP-1042")
    @Size(max = 30, message = "Employee code must not exceed 30 characters")
    private String employeeCode;

    private UUID departmentId;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    @NotNull(message = "Date of joining is required")
    @PastOrPresent(message = "Date of joining cannot be in the future")
    private LocalDate dateOfJoining;

    private UUID reportingManagerId;

    private BigDecimal annualCtc;

    @Valid
    private AddressDto address;

    @Valid
    private EmergencyContactDto emergencyContact;
}

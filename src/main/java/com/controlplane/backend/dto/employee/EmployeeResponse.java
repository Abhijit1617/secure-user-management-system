package com.controlplane.backend.dto.employee;

import com.controlplane.backend.dto.user.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private UUID id;
    private String employeeCode;
    private UUID organizationId;
    private UserSummaryResponse user;
    private UUID departmentId;
    private String departmentName;
    private String designation;
    private String employmentType;
    private String status;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;
    private UUID reportingManagerId;
    private String reportingManagerName;
    private BigDecimal annualCtc;
    private AddressDto address;
    private EmergencyContactDto emergencyContact;
    private Instant createdAt;
}

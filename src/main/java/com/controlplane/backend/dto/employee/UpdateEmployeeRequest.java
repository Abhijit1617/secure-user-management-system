package com.controlplane.backend.dto.employee;

import com.controlplane.backend.entity.enums.EmploymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {

    private UUID departmentId;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    private UUID reportingManagerId;

    private BigDecimal annualCtc;
}

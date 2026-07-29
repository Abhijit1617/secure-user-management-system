package com.controlplane.backend.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSummaryResponse {

    private UUID id;
    private String employeeCode;
    private String fullName;
    private String designation;
    private String departmentName;
    private String status;
}

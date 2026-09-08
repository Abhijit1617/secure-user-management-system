package com.controlplane.backend.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentStatisticsResponse {

    private UUID departmentId;
    private String departmentName;
    private long totalEmployees;
    private long activeEmployees;
    private long projectCount;
    private long childDepartmentCount;
}

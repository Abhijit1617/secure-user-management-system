package com.controlplane.backend.dto.project;

import com.controlplane.backend.dto.employee.EmployeeSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private String status;
    private String priority;
    private UUID departmentId;
    private String departmentName;
    private EmployeeSummaryResponse projectManager;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<EmployeeSummaryResponse> members;
    private int taskCount;
    private Instant createdAt;
}

package com.controlplane.backend.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String code;
    private String description;
    private UUID parentDepartmentId;
    private String parentDepartmentName;
    private EmployeeRefResponse head;
    private boolean active;
    private int employeeCount;
    private int childDepartmentCount;
    private Instant createdAt;
}

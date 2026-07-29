package com.controlplane.backend.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTreeResponse {

    private UUID id;
    private String name;
    private String code;
    private boolean active;
    private int employeeCount;
    private List<DepartmentTreeResponse> children;
}

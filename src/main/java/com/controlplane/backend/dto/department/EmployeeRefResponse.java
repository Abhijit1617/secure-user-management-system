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
public class EmployeeRefResponse {

    private UUID id;
    private String employeeCode;
    private String fullName;
}

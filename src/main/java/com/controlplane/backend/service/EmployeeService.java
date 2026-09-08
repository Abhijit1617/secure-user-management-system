package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.employee.CreateEmployeeRequest;
import com.controlplane.backend.dto.employee.EmployeeResponse;
import com.controlplane.backend.dto.employee.UpdateEmployeeProfileRequest;
import com.controlplane.backend.dto.employee.UpdateEmployeeRequest;
import com.controlplane.backend.dto.employee.UpdateEmployeeStatusRequest;
import com.controlplane.backend.entity.enums.EmployeeStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EmployeeService {

    PageResponse<EmployeeResponse> search(UUID organizationId, UUID departmentId, EmployeeStatus status,
                                           String searchTerm, Pageable pageable);

    EmployeeResponse getById(UUID id);

    EmployeeResponse create(CreateEmployeeRequest request);

    EmployeeResponse update(UUID id, UpdateEmployeeRequest request);

    EmployeeResponse updateProfile(UUID id, UpdateEmployeeProfileRequest request);

    EmployeeResponse updateStatus(UUID id, UpdateEmployeeStatusRequest request);

    void delete(UUID id);
}

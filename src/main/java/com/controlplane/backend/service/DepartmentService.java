package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.department.AssignDepartmentManagerRequest;
import com.controlplane.backend.dto.department.CreateDepartmentRequest;
import com.controlplane.backend.dto.department.DepartmentResponse;
import com.controlplane.backend.dto.department.DepartmentStatisticsResponse;
import com.controlplane.backend.dto.department.DepartmentTreeResponse;
import com.controlplane.backend.dto.department.UpdateDepartmentRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    PageResponse<DepartmentResponse> search(UUID organizationId, String searchTerm, Pageable pageable);

    DepartmentResponse getById(UUID id);

    List<DepartmentTreeResponse> getHierarchy(UUID organizationId);

    DepartmentStatisticsResponse getStatistics(UUID id);

    DepartmentResponse create(CreateDepartmentRequest request);

    DepartmentResponse update(UUID id, UpdateDepartmentRequest request);

    DepartmentResponse assignManager(UUID id, AssignDepartmentManagerRequest request);

    DepartmentResponse removeManager(UUID id);

    DepartmentResponse activate(UUID id);

    DepartmentResponse deactivate(UUID id);

    void delete(UUID id);
}

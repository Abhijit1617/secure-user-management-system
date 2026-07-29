package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.permission.PermissionRequest;
import com.controlplane.backend.dto.permission.PermissionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PermissionService {

    PageResponse<PermissionResponse> search(String searchTerm, Pageable pageable);

    PermissionResponse getById(UUID id);

    PermissionResponse create(PermissionRequest request);

    PermissionResponse update(UUID id, PermissionRequest request);

    void delete(UUID id);
}

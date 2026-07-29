package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.permission.PermissionRequest;
import com.controlplane.backend.dto.permission.PermissionResponse;
import com.controlplane.backend.entity.Permission;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.PermissionMapper;
import com.controlplane.backend.repository.PermissionRepository;
import com.controlplane.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> search(String searchTerm, Pageable pageable) {
        Page<Permission> page = StringUtils.hasText(searchTerm)
                ? permissionRepository.findByNameContainingIgnoreCaseOrModuleContainingIgnoreCase(
                        searchTerm, searchTerm, pageable)
                : permissionRepository.findAll(pageable);
        return PageResponse.from(page.map(permissionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getById(UUID id) {
        return permissionMapper.toResponse(loadPermission(id));
    }

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw DuplicateResourceException.of("Permission", "name", request.getName());
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .module(request.getModule())
                .build();

        permission = permissionRepository.save(permission);
        log.info("Created permission [{}]", permission.getName());
        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional
    public PermissionResponse update(UUID id, PermissionRequest request) {
        Permission permission = loadPermission(id);

        if (!permission.getName().equals(request.getName())
                && permissionRepository.existsByName(request.getName())) {
            throw DuplicateResourceException.of("Permission", "name", request.getName());
        }

        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setModule(request.getModule());

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Permission permission = loadPermission(id);
        permissionRepository.delete(permission);
        log.info("Deleted permission [{}]", permission.getName());
    }

    private Permission loadPermission(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", "id", id));
    }
}

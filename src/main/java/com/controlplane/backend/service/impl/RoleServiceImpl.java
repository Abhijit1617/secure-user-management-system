package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.role.AssignPermissionsRequest;
import com.controlplane.backend.dto.role.RoleRequest;
import com.controlplane.backend.dto.role.RoleResponse;
import com.controlplane.backend.entity.Permission;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.RoleMapper;
import com.controlplane.backend.repository.PermissionRepository;
import com.controlplane.backend.repository.RoleRepository;
import com.controlplane.backend.service.RoleService;
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
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> search(String searchTerm, Pageable pageable) {
        Page<Role> page = StringUtils.hasText(searchTerm)
                ? roleRepository.findByNameContainingIgnoreCase(searchTerm, pageable)
                : roleRepository.findAll(pageable);
        return PageResponse.from(page.map(roleMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {
        return roleMapper.toResponse(loadRole(id));
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw DuplicateResourceException.of("Role", "name", request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .hierarchyLevel(request.getHierarchyLevel())
                .build();

        if (request.getPermissionIds() != null) {
            for (UUID permissionId : request.getPermissionIds()) {
                role.addPermission(loadPermission(permissionId));
            }
        }

        role = roleRepository.save(role);
        log.info("Created role [{}]", role.getName());
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, RoleRequest request) {
        Role role = loadRole(id);

        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw DuplicateResourceException.of("Role", "name", request.getName());
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setHierarchyLevel(request.getHierarchyLevel());

        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Role role = loadRole(id);
        roleRepository.delete(role);
        log.info("Deleted role [{}]", role.getName());
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(UUID id, AssignPermissionsRequest request) {
        Role role = loadRole(id);
        for (UUID permissionId : request.getPermissionIds()) {
            role.addPermission(loadPermission(permissionId));
        }
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse removePermission(UUID id, UUID permissionId) {
        Role role = loadRole(id);
        role.removePermission(loadPermission(permissionId));
        return roleMapper.toResponse(roleRepository.save(role));
    }

    private Role loadRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", "id", id));
    }

    private Permission loadPermission(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", "id", id));
    }
}

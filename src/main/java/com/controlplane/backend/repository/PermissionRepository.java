package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    Page<Permission> findByNameContainingIgnoreCaseOrModuleContainingIgnoreCase(
            String name, String module, Pageable pageable);
}

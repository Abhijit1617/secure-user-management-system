package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Organization> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
            String name, String slug, Pageable pageable);

    Page<Organization> findByOwner(User owner, Pageable pageable);
}

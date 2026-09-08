package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    boolean existsByCode(String code);

    @Query("select p from Project p where p.department.organization = :organization")
    List<Project> findAllByOrganization(@Param("organization") Organization organization);
}

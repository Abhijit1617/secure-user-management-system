package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByUserId(UUID userId);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByUserId(UUID userId);

    long countByOrganization(Organization organization);
}


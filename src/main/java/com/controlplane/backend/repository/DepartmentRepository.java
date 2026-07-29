package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Department;
import com.controlplane.backend.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    long countByOrganization(Organization organization);

    Optional<Department> findByOrganizationAndCode(Organization organization, String code);

    boolean existsByOrganizationAndCode(Organization organization, String code);

    Page<Department> findByOrganization(Organization organization, Pageable pageable);

    @Query("select d from Department d where d.organization = :organization "
            + "and (lower(d.name) like lower(concat('%', :searchTerm, '%')) "
            + "or lower(d.code) like lower(concat('%', :searchTerm, '%')))")
    Page<Department> search(@Param("organization") Organization organization,
                             @Param("searchTerm") String searchTerm,
                             Pageable pageable);

    List<Department> findByParentDepartmentIsNullAndOrganization(Organization organization);

    List<Department> findByParentDepartment(Department parentDepartment);

    @Query("select count(e) from Employee e where e.department.id = :departmentId "
            + "and e.status = com.controlplane.backend.entity.enums.EmployeeStatus.ACTIVE")
    long countActiveEmployees(@Param("departmentId") UUID departmentId);

    @Query("select count(p) from Project p where p.department.id = :departmentId")
    long countProjects(@Param("departmentId") UUID departmentId);
}

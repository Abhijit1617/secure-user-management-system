package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.department.AssignDepartmentManagerRequest;
import com.controlplane.backend.dto.department.CreateDepartmentRequest;
import com.controlplane.backend.dto.department.DepartmentResponse;
import com.controlplane.backend.dto.department.DepartmentStatisticsResponse;
import com.controlplane.backend.dto.department.DepartmentTreeResponse;
import com.controlplane.backend.dto.department.UpdateDepartmentRequest;
import com.controlplane.backend.entity.Department;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.exception.DepartmentNotEmptyException;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.InvalidRequestException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.DepartmentMapper;
import com.controlplane.backend.repository.DepartmentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.OrganizationRepository;
import com.controlplane.backend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

import static com.controlplane.backend.config.RedisCacheConfig.DEPARTMENT_CACHE;
import static com.controlplane.backend.config.RedisCacheConfig.ORGANIZATION_CACHE;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> search(UUID organizationId, String searchTerm, Pageable pageable) {
        Organization organization = loadOrganization(organizationId);
        Page<Department> page = StringUtils.hasText(searchTerm)
                ? departmentRepository.search(organization, searchTerm, pageable)
                : departmentRepository.findByOrganization(organization, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public DepartmentResponse getById(UUID id) {
        return toResponse(loadDepartment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentTreeResponse> getHierarchy(UUID organizationId) {
        Organization organization = loadOrganization(organizationId);
        List<Department> roots = departmentRepository.findByParentDepartmentIsNullAndOrganization(organization);
        return roots.stream().map(this::buildTreeNode).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentStatisticsResponse getStatistics(UUID id) {
        Department department = loadDepartment(id);
        long activeEmployees = departmentRepository.countActiveEmployees(department.getId());
        long projectCount = departmentRepository.countProjects(department.getId());
        long childCount = departmentRepository.findByParentDepartment(department).size();

        return DepartmentStatisticsResponse.builder()
                .departmentId(department.getId())
                .departmentName(department.getName())
                .totalEmployees(department.getEmployees().size())
                .activeEmployees(activeEmployees)
                .projectCount(projectCount)
                .childDepartmentCount(childCount)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = ORGANIZATION_CACHE, key = "#request.organizationId")
    public DepartmentResponse create(CreateDepartmentRequest request) {
        Organization organization = loadOrganization(request.getOrganizationId());

        if (departmentRepository.existsByOrganizationAndCode(organization, request.getCode())) {
            throw DuplicateResourceException.of("Department", "code", request.getCode());
        }

        Department parent = null;
        if (request.getParentDepartmentId() != null) {
            parent = loadDepartment(request.getParentDepartmentId());
        }

        Department department = Department.builder()
                .organization(organization)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .parentDepartment(parent)
                .active(true)
                .build();

        department = departmentRepository.save(department);
        log.info("Created department [{}] in organization [{}]", department.getCode(), organization.getSlug());
        return toResponse(department);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public DepartmentResponse update(UUID id, UpdateDepartmentRequest request) {
        Department department = loadDepartment(id);

        if (!department.getCode().equals(request.getCode())
                && departmentRepository.existsByOrganizationAndCode(department.getOrganization(), request.getCode())) {
            throw DuplicateResourceException.of("Department", "code", request.getCode());
        }

        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setDescription(request.getDescription());

        if (request.getParentDepartmentId() != null) {
            if (request.getParentDepartmentId().equals(department.getId())) {
                throw new InvalidRequestException("A department cannot be its own parent");
            }
            department.setParentDepartment(loadDepartment(request.getParentDepartmentId()));
        } else {
            department.setParentDepartment(null);
        }

        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public DepartmentResponse assignManager(UUID id, AssignDepartmentManagerRequest request) {
        Department department = loadDepartment(id);
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", request.getEmployeeId()));
        department.setHead(employee);
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public DepartmentResponse removeManager(UUID id) {
        Department department = loadDepartment(id);
        department.setHead(null);
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public DepartmentResponse activate(UUID id) {
        Department department = loadDepartment(id);
        department.setActive(true);
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public DepartmentResponse deactivate(UUID id) {
        Department department = loadDepartment(id);
        department.setActive(false);
        return toResponse(departmentRepository.save(department));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = DEPARTMENT_CACHE, key = "#id")
    public void delete(UUID id) {
        Department department = loadDepartment(id);
        long employeeCount = department.getEmployees().size();
        long childCount = departmentRepository.findByParentDepartment(department).size();
        if (employeeCount > 0 || childCount > 0) {
            throw new DepartmentNotEmptyException(employeeCount, childCount);
        }
        departmentRepository.delete(department);
        log.info("Deleted department [{}]", department.getCode());
    }

    private DepartmentTreeResponse buildTreeNode(Department department) {
        List<Department> children = departmentRepository.findByParentDepartment(department);
        List<DepartmentTreeResponse> childNodes = children.stream().map(this::buildTreeNode).toList();
        int employeeCount = department.getEmployees().size();
        return departmentMapper.toTreeNode(department, employeeCount, childNodes);
    }

    private DepartmentResponse toResponse(Department department) {
        int employeeCount = department.getEmployees().size();
        int childCount = departmentRepository.findByParentDepartment(department).size();
        return departmentMapper.toResponse(department, employeeCount, childCount);
    }

    private Department loadDepartment(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", "id", id));
    }

    private Organization loadOrganization(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", "id", id));
    }
}

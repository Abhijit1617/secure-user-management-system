package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.employee.CreateEmployeeRequest;
import com.controlplane.backend.dto.employee.EmployeeResponse;
import com.controlplane.backend.dto.employee.UpdateEmployeeProfileRequest;
import com.controlplane.backend.dto.employee.UpdateEmployeeRequest;
import com.controlplane.backend.dto.employee.UpdateEmployeeStatusRequest;
import com.controlplane.backend.entity.Department;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EmployeeStatus;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.InvalidRequestException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.EmployeeMapper;
import com.controlplane.backend.repository.DepartmentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.OrganizationRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.repository.spec.EmployeeSpecifications;
import com.controlplane.backend.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.controlplane.backend.config.RedisCacheConfig.EMPLOYEE_CACHE;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(UUID organizationId, UUID departmentId, EmployeeStatus status,
                                                  String searchTerm, Pageable pageable) {
        Organization organization = loadOrganization(organizationId);
        Specification<Employee> spec = EmployeeSpecifications.combine(
                EmployeeSpecifications.belongsToOrganization(organization),
                EmployeeSpecifications.hasDepartment(departmentId),
                EmployeeSpecifications.hasStatus(status),
                EmployeeSpecifications.searchTermMatches(searchTerm)
        );
        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(employeeMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = EMPLOYEE_CACHE, key = "#id")
    public EmployeeResponse getById(UUID id) {
        return employeeMapper.toResponse(loadEmployee(id));
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Organization organization = loadOrganization(request.getOrganizationId());

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw DuplicateResourceException.of("Employee", "employeeCode", request.getEmployeeCode());
        }
        if (employeeRepository.existsByUserId(request.getUserId())) {
            throw new InvalidRequestException("This user account is already linked to an employee record");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", request.getUserId()));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Department", "id", request.getDepartmentId()));
        }

        Employee reportingManager = resolveManager(request.getReportingManagerId(), null);

        Employee employee = Employee.builder()
                .organization(organization)
                .employeeCode(request.getEmployeeCode())
                .user(user)
                .department(department)
                .designation(request.getDesignation())
                .employmentType(request.getEmploymentType())
                .status(EmployeeStatus.ACTIVE)
                .dateOfJoining(request.getDateOfJoining())
                .reportingManager(reportingManager)
                .annualCtc(request.getAnnualCtc())
                .address(employeeMapper.toAddressEntity(request.getAddress()))
                .emergencyContact(employeeMapper.toEmergencyContactEntity(request.getEmergencyContact()))
                .build();

        employee = employeeRepository.save(employee);
        log.info("Created employee [{}] in organization [{}]", employee.getEmployeeCode(), organization.getSlug());
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = EMPLOYEE_CACHE, key = "#id")
    public EmployeeResponse update(UUID id, UpdateEmployeeRequest request) {
        Employee employee = loadEmployee(id);

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Department", "id", request.getDepartmentId()));
        }

        Employee reportingManager = resolveManager(request.getReportingManagerId(), employee.getId());

        employee.setDepartment(department);
        employee.setDesignation(request.getDesignation());
        employee.setEmploymentType(request.getEmploymentType());
        employee.setReportingManager(reportingManager);
        employee.setAnnualCtc(request.getAnnualCtc());

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = EMPLOYEE_CACHE, key = "#id")
    public EmployeeResponse updateProfile(UUID id, UpdateEmployeeProfileRequest request) {
        Employee employee = loadEmployee(id);
        if (request.getAddress() != null) {
            employee.setAddress(employeeMapper.toAddressEntity(request.getAddress()));
        }
        if (request.getEmergencyContact() != null) {
            employee.setEmergencyContact(employeeMapper.toEmergencyContactEntity(request.getEmergencyContact()));
        }
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = EMPLOYEE_CACHE, key = "#id")
    public EmployeeResponse updateStatus(UUID id, UpdateEmployeeStatusRequest request) {
        Employee employee = loadEmployee(id);
        employee.setStatus(request.getStatus());
        if (request.getStatus() == EmployeeStatus.RESIGNED || request.getStatus() == EmployeeStatus.TERMINATED) {
            employee.setDateOfExit(request.getDateOfExit() != null
                    ? request.getDateOfExit() : java.time.LocalDate.now());
        }
        log.info("Employee [{}] status changed to {}", employee.getEmployeeCode(), request.getStatus());
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = EMPLOYEE_CACHE, key = "#id")
    public void delete(UUID id) {
        Employee employee = loadEmployee(id);
        employeeRepository.delete(employee);
        log.info("Deleted employee [{}]", employee.getEmployeeCode());
    }

    private Employee resolveManager(UUID managerId, UUID selfId) {
        if (managerId == null) {
            return null;
        }
        if (managerId.equals(selfId)) {
            throw new InvalidRequestException("An employee cannot be their own reporting manager");
        }
        return employeeRepository.findById(managerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", managerId));
    }

    private Employee loadEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", id));
    }

    private Organization loadOrganization(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", "id", id));
    }
}

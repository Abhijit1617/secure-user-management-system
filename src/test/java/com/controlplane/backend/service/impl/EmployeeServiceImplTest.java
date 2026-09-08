package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.employee.UpdateEmployeeRequest;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EmploymentType;
import com.controlplane.backend.exception.InvalidRequestException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.EmployeeMapper;
import com.controlplane.backend.repository.DepartmentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.OrganizationRepository;
import com.controlplane.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        Organization organization = Organization.builder().slug("acme").build();
        organization.setId(UUID.randomUUID());
        User user = User.builder().username("jane.doe").build();
        user.setId(UUID.randomUUID());
        employee = Employee.builder()
                .organization(organization)
                .employeeCode("EMP-001")
                .user(user)
                .employmentType(EmploymentType.FULL_TIME)
                .dateOfJoining(LocalDate.now())
                .build();
        employee.setId(employeeId);
        lenient().when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getByIdThrowsWhenEmployeeNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(employeeRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsSelfAsReportingManager() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .employmentType(EmploymentType.FULL_TIME)
                .reportingManagerId(employeeId)
                .build();

        assertThatThrownBy(() -> employeeService.update(employeeId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("own reporting manager");
    }

    @Test
    void updateAppliesNewDesignationAndEmploymentType() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .designation("Senior Engineer")
                .employmentType(EmploymentType.CONTRACT)
                .build();

        employeeService.update(employeeId, request);

        assertThat(employee.getDesignation()).isEqualTo("Senior Engineer");
        assertThat(employee.getEmploymentType()).isEqualTo(EmploymentType.CONTRACT);
    }
}

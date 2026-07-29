package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.project.CreateProjectRequest;
import com.controlplane.backend.dto.project.UpdateProjectTimelineRequest;
import com.controlplane.backend.entity.Department;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.Project;
import com.controlplane.backend.entity.enums.Priority;
import com.controlplane.backend.entity.enums.ProjectStatus;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.InvalidRequestException;
import com.controlplane.backend.mapper.ProjectMapper;
import com.controlplane.backend.repository.DepartmentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.OrganizationRepository;
import com.controlplane.backend.repository.ProjectRepository;
import com.controlplane.backend.repository.TaskRepository;
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
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Department department;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        Organization organization = Organization.builder().slug("acme").build();
        organization.setId(UUID.randomUUID());
        department = Department.builder().organization(organization).code("ENG").build();
        department.setId(UUID.randomUUID());
        projectId = UUID.randomUUID();
        project = Project.builder()
                .department(department)
                .name("Platform Revamp")
                .code("PRJ-1")
                .status(ProjectStatus.PLANNED)
                .priority(Priority.MEDIUM)
                .build();
        project.setId(projectId);
        lenient().when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(taskRepository.countByProject(any(Project.class))).thenReturn(0L);
    }

    @Test
    void createRejectsDuplicateCode() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .departmentId(department.getId())
                .name("Platform Revamp")
                .code("PRJ-1")
                .priority(Priority.MEDIUM)
                .build();

        when(projectRepository.existsByCode("PRJ-1")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .departmentId(department.getId())
                .name("Platform Revamp")
                .code("PRJ-2")
                .priority(Priority.MEDIUM)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        when(projectRepository.existsByCode("PRJ-2")).thenReturn(false);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateTimelineRejectsEndDateBeforeStartDate() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        UpdateProjectTimelineRequest request = UpdateProjectTimelineRequest.builder()
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now())
                .build();

        assertThatThrownBy(() -> projectService.updateTimeline(projectId, request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateTimelineAppliesValidDates() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(30);

        UpdateProjectTimelineRequest request = UpdateProjectTimelineRequest.builder()
                .startDate(start)
                .endDate(end)
                .build();

        projectService.updateTimeline(projectId, request);

        assertThat(project.getStartDate()).isEqualTo(start);
        assertThat(project.getEndDate()).isEqualTo(end);
    }
}

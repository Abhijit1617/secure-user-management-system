package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.project.CreateProjectRequest;
import com.controlplane.backend.dto.project.ProjectDashboardResponse;
import com.controlplane.backend.dto.project.ProjectMembersRequest;
import com.controlplane.backend.dto.project.ProjectResponse;
import com.controlplane.backend.dto.project.UpdateProjectPriorityRequest;
import com.controlplane.backend.dto.project.UpdateProjectRequest;
import com.controlplane.backend.dto.project.UpdateProjectStatusRequest;
import com.controlplane.backend.dto.project.UpdateProjectTimelineRequest;
import com.controlplane.backend.entity.Department;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.Project;
import com.controlplane.backend.entity.enums.Priority;
import com.controlplane.backend.entity.enums.ProjectStatus;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.InvalidRequestException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.ProjectMapper;
import com.controlplane.backend.repository.DepartmentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.OrganizationRepository;
import com.controlplane.backend.repository.ProjectRepository;
import com.controlplane.backend.repository.TaskRepository;
import com.controlplane.backend.repository.spec.ProjectSpecifications;
import com.controlplane.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.controlplane.backend.config.RedisCacheConfig.DASHBOARD_CACHE;
import static com.controlplane.backend.config.RedisCacheConfig.PROJECT_CACHE;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private static final int UPCOMING_DEADLINE_WINDOW_DAYS = 14;
    private static final int UPCOMING_DEADLINE_LIMIT = 5;

    private final ProjectRepository projectRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> search(UUID organizationId, UUID departmentId, ProjectStatus status,
                                                 Priority priority, String searchTerm, Pageable pageable) {
        Organization organization = loadOrganization(organizationId);
        Specification<Project> spec = ProjectSpecifications.combine(
                ProjectSpecifications.belongsToOrganization(organization),
                ProjectSpecifications.hasDepartment(departmentId),
                ProjectSpecifications.hasStatus(status),
                ProjectSpecifications.hasPriority(priority),
                ProjectSpecifications.searchTermMatches(searchTerm)
        );
        Page<Project> page = projectRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = PROJECT_CACHE, key = "#id")
    public ProjectResponse getById(UUID id) {
        return toResponse(loadProject(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = DASHBOARD_CACHE, key = "'projects:' + #organizationId")
    public ProjectDashboardResponse getDashboard(UUID organizationId) {
        Organization organization = loadOrganization(organizationId);
        List<Project> projects = projectRepository.findAllByOrganization(organization);
        LocalDate today = LocalDate.now();

        var countByStatus = projects.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));
        var countByPriority = projects.stream()
                .collect(Collectors.groupingBy(p -> p.getPriority().name(), Collectors.counting()));

        long overdueCount = projects.stream()
                .filter(p -> p.getEndDate() != null && p.getEndDate().isBefore(today)
                        && p.getStatus() != ProjectStatus.COMPLETED && p.getStatus() != ProjectStatus.CANCELLED)
                .count();

        LocalDate windowEnd = today.plusDays(UPCOMING_DEADLINE_WINDOW_DAYS);
        var upcoming = projects.stream()
                .filter(p -> p.getEndDate() != null && !p.getEndDate().isBefore(today)
                        && !p.getEndDate().isAfter(windowEnd)
                        && p.getStatus() != ProjectStatus.COMPLETED && p.getStatus() != ProjectStatus.CANCELLED)
                .sorted((a, b) -> a.getEndDate().compareTo(b.getEndDate()))
                .limit(UPCOMING_DEADLINE_LIMIT)
                .map(projectMapper::toSummaryResponse)
                .toList();

        return ProjectDashboardResponse.builder()
                .totalProjects(projects.size())
                .countByStatus(countByStatus)
                .countByPriority(countByPriority)
                .overdueProjects(overdueCount)
                .upcomingDeadlines(upcoming)
                .build();
    }

    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        if (projectRepository.existsByCode(request.getCode())) {
            throw DuplicateResourceException.of("Project", "code", request.getCode());
        }
        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidRequestException("End date cannot be before start date");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Department", "id", request.getDepartmentId()));

        Employee manager = null;
        if (request.getProjectManagerId() != null) {
            manager = employeeRepository.findById(request.getProjectManagerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", request.getProjectManagerId()));
        }

        Project project = Project.builder()
                .department(department)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .status(ProjectStatus.PLANNED)
                .priority(request.getPriority())
                .projectManager(manager)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        project = projectRepository.save(project);
        log.info("Created project [{}] in department [{}]", project.getCode(), department.getCode());
        return toResponse(project);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = DASHBOARD_CACHE, allEntries = true)
    })
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = loadProject(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        if (request.getProjectManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getProjectManagerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", request.getProjectManagerId()));
            project.setProjectManager(manager);
        } else {
            project.setProjectManager(null);
        }

        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = DASHBOARD_CACHE, allEntries = true)
    })
    public ProjectResponse updateStatus(UUID id, UpdateProjectStatusRequest request) {
        Project project = loadProject(id);
        project.setStatus(request.getStatus());
        log.info("Project [{}] status changed to {}", project.getCode(), request.getStatus());
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = DASHBOARD_CACHE, allEntries = true)
    })
    public ProjectResponse updatePriority(UUID id, UpdateProjectPriorityRequest request) {
        Project project = loadProject(id);
        project.setPriority(request.getPriority());
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id")
    public ProjectResponse updateTimeline(UUID id, UpdateProjectTimelineRequest request) {
        Project project = loadProject(id);
        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidRequestException("End date cannot be before start date");
        }
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id")
    public ProjectResponse addMembers(UUID id, ProjectMembersRequest request) {
        Project project = loadProject(id);
        Set<Employee> employees = new HashSet<>();
        for (UUID employeeId : request.getEmployeeIds()) {
            employees.add(employeeRepository.findById(employeeId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", employeeId)));
        }
        employees.forEach(project::addMember);
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id")
    public ProjectResponse removeMember(UUID id, UUID employeeId) {
        Project project = loadProject(id);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", employeeId));
        project.removeMember(employee);
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = PROJECT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = DASHBOARD_CACHE, allEntries = true)
    })
    public void delete(UUID id) {
        Project project = loadProject(id);
        projectRepository.delete(project);
        log.info("Deleted project [{}]", project.getCode());
    }

    private ProjectResponse toResponse(Project project) {
        int taskCount = (int) taskRepository.countByProject(project);
        return projectMapper.toResponse(project, taskCount);
    }

    private Project loadProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", "id", id));
    }

    private Organization loadOrganization(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", "id", id));
    }
}

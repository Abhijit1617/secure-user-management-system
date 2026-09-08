package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.project.ProjectResponse;
import com.controlplane.backend.dto.project.ProjectSummaryResponse;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final EmployeeMapper employeeMapper;

    public ProjectResponse toResponse(Project project, int taskCount) {
        Employee manager = project.getProjectManager();
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .code(project.getCode())
                .description(project.getDescription())
                .status(project.getStatus().name())
                .priority(project.getPriority().name())
                .departmentId(project.getDepartment() != null ? project.getDepartment().getId() : null)
                .departmentName(project.getDepartment() != null ? project.getDepartment().getName() : null)
                .projectManager(manager != null ? employeeMapper.toSummaryResponse(manager) : null)
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .members(project.getMembers().stream()
                        .map(employeeMapper::toSummaryResponse)
                        .collect(Collectors.toSet()))
                .taskCount(taskCount)
                .createdAt(project.getCreatedAt())
                .build();
    }

    public ProjectSummaryResponse toSummaryResponse(Project project) {
        return ProjectSummaryResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .code(project.getCode())
                .status(project.getStatus().name())
                .priority(project.getPriority().name())
                .endDate(project.getEndDate())
                .build();
    }
}

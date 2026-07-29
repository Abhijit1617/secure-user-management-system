package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.project.CreateProjectRequest;
import com.controlplane.backend.dto.project.ProjectDashboardResponse;
import com.controlplane.backend.dto.project.ProjectMembersRequest;
import com.controlplane.backend.dto.project.ProjectResponse;
import com.controlplane.backend.dto.project.UpdateProjectPriorityRequest;
import com.controlplane.backend.dto.project.UpdateProjectRequest;
import com.controlplane.backend.dto.project.UpdateProjectStatusRequest;
import com.controlplane.backend.dto.project.UpdateProjectTimelineRequest;
import com.controlplane.backend.entity.enums.Priority;
import com.controlplane.backend.entity.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProjectService {

    PageResponse<ProjectResponse> search(UUID organizationId, UUID departmentId, ProjectStatus status,
                                          Priority priority, String searchTerm, Pageable pageable);

    ProjectResponse getById(UUID id);

    ProjectDashboardResponse getDashboard(UUID organizationId);

    ProjectResponse create(CreateProjectRequest request);

    ProjectResponse update(UUID id, UpdateProjectRequest request);

    ProjectResponse updateStatus(UUID id, UpdateProjectStatusRequest request);

    ProjectResponse updatePriority(UUID id, UpdateProjectPriorityRequest request);

    ProjectResponse updateTimeline(UUID id, UpdateProjectTimelineRequest request);

    ProjectResponse addMembers(UUID id, ProjectMembersRequest request);

    ProjectResponse removeMember(UUID id, UUID employeeId);

    void delete(UUID id);
}

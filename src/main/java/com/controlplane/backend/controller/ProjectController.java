package com.controlplane.backend.controller;

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
import com.controlplane.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Project Management", description = "Project CRUD, members, status, priority, timeline and dashboard")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Search projects with department/status/priority filters and pagination")
    public ResponseEntity<PageResponse<ProjectResponse>> search(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                projectService.search(organizationId, departmentId, status, priority, searchTerm, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Get a project by id, including members and task count")
    public ResponseEntity<ProjectResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PROJECT_READ')")
    @Operation(summary = "Get project dashboard aggregates for an organization",
            description = "Counts by status and priority, overdue project count, and the next 5 upcoming deadlines.")
    public ResponseEntity<ProjectDashboardResponse> getDashboard(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(projectService.getDashboard(organizationId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(summary = "Create a new project under a department")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Update a project's name, description and manager")
    public ResponseEntity<ProjectResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Change a project's status")
    public ResponseEntity<ProjectResponse> updateStatus(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateProjectStatusRequest request) {
        return ResponseEntity.ok(projectService.updateStatus(id, request));
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Change a project's priority")
    public ResponseEntity<ProjectResponse> updatePriority(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateProjectPriorityRequest request) {
        return ResponseEntity.ok(projectService.updatePriority(id, request));
    }

    @PutMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Update a project's start and end dates")
    public ResponseEntity<ProjectResponse> updateTimeline(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateProjectTimelineRequest request) {
        return ResponseEntity.ok(projectService.updateTimeline(id, request));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Add one or more employees to a project")
    public ResponseEntity<ProjectResponse> addMembers(@PathVariable UUID id,
                                                       @Valid @RequestBody ProjectMembersRequest request) {
        return ResponseEntity.ok(projectService.addMembers(id, request));
    }

    @DeleteMapping("/{id}/members/{employeeId}")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @Operation(summary = "Remove an employee from a project")
    public ResponseEntity<ProjectResponse> removeMember(@PathVariable UUID id, @PathVariable UUID employeeId) {
        return ResponseEntity.ok(projectService.removeMember(id, employeeId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    @Operation(summary = "Delete a project")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

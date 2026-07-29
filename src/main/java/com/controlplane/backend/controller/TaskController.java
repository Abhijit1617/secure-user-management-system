package com.controlplane.backend.controller;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.task.AssignTaskRequest;
import com.controlplane.backend.dto.task.CreateTaskRequest;
import com.controlplane.backend.dto.task.TaskHistoryResponse;
import com.controlplane.backend.dto.task.TaskResponse;
import com.controlplane.backend.dto.task.UpdateTaskDueDateRequest;
import com.controlplane.backend.dto.task.UpdateTaskPriorityRequest;
import com.controlplane.backend.dto.task.UpdateTaskRequest;
import com.controlplane.backend.dto.task.UpdateTaskStatusRequest;
import com.controlplane.backend.entity.enums.TaskStatus;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.service.TaskService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Task Management", description = "Task CRUD, assignment, status/priority/due date, and change history")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAuthority('TASK_READ')")
    @Operation(summary = "Search tasks within a project, with assignee/status filters and pagination")
    public ResponseEntity<PageResponse<TaskResponse>> search(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return ResponseEntity.ok(taskService.search(projectId, assigneeId, status, searchTerm, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_READ')")
    @Operation(summary = "Get a task by id, including comment and attachment counts")
    public ResponseEntity<TaskResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TASK_CREATE')")
    @Operation(summary = "Create a task under a project")
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_UPDATE')")
    @Operation(summary = "Update a task's title, description and estimated hours")
    public ResponseEntity<TaskResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('TASK_ASSIGN')")
    @Operation(summary = "Assign or reassign a task to an employee")
    public ResponseEntity<TaskResponse> assign(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskService.assign(id, request, principal.getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TASK_UPDATE')")
    @Operation(summary = "Change a task's status")
    public ResponseEntity<TaskResponse> updateStatus(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(id, request, principal.getId()));
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('TASK_UPDATE')")
    @Operation(summary = "Change a task's priority")
    public ResponseEntity<TaskResponse> updatePriority(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody UpdateTaskPriorityRequest request) {
        return ResponseEntity.ok(taskService.updatePriority(id, request, principal.getId()));
    }

    @PutMapping("/{id}/due-date")
    @PreAuthorize("hasAuthority('TASK_UPDATE')")
    @Operation(summary = "Change a task's due date")
    public ResponseEntity<TaskResponse> updateDueDate(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @PathVariable UUID id,
                                                       @Valid @RequestBody UpdateTaskDueDateRequest request) {
        return ResponseEntity.ok(taskService.updateDueDate(id, request, principal.getId()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('TASK_READ')")
    @Operation(summary = "Get the full change history for a task (status, assignee, priority, due date)")
    public ResponseEntity<List<TaskHistoryResponse>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getHistory(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TASK_DELETE')")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

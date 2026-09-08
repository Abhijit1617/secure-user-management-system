package com.controlplane.backend.service;

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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TaskService {

    PageResponse<TaskResponse> search(UUID projectId, UUID assigneeId, TaskStatus status, String searchTerm,
                                       Pageable pageable);

    TaskResponse getById(UUID id);

    TaskResponse create(CreateTaskRequest request, UUID requesterId);

    TaskResponse update(UUID id, UpdateTaskRequest request);

    TaskResponse assign(UUID id, AssignTaskRequest request, UUID requesterId);

    TaskResponse updateStatus(UUID id, UpdateTaskStatusRequest request, UUID requesterId);

    TaskResponse updatePriority(UUID id, UpdateTaskPriorityRequest request, UUID requesterId);

    TaskResponse updateDueDate(UUID id, UpdateTaskDueDateRequest request, UUID requesterId);

    List<TaskHistoryResponse> getHistory(UUID id);

    void delete(UUID id);
}

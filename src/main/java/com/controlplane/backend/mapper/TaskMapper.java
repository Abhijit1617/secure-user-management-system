package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.task.TaskHistoryResponse;
import com.controlplane.backend.dto.task.TaskResponse;
import com.controlplane.backend.dto.task.TaskSummaryResponse;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Task;
import com.controlplane.backend.entity.TaskHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final EmployeeMapper employeeMapper;

    public TaskResponse toResponse(Task task, long commentCount, long attachmentCount) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .assignee(task.getAssignee() != null ? employeeMapper.toSummaryResponse(task.getAssignee()) : null)
                .reporter(task.getReporter() != null ? employeeMapper.toSummaryResponse(task.getReporter()) : null)
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .estimatedHours(task.getEstimatedHours())
                .commentCount(commentCount)
                .attachmentCount(attachmentCount)
                .createdAt(task.getCreatedAt())
                .build();
    }

    public TaskSummaryResponse toSummaryResponse(Task task) {
        Employee assignee = task.getAssignee();
        return TaskSummaryResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .dueDate(task.getDueDate())
                .assigneeName(assignee != null && assignee.getUser() != null
                        ? assignee.getUser().getFullName() : null)
                .build();
    }

    public TaskHistoryResponse toHistoryResponse(TaskHistory history) {
        return TaskHistoryResponse.builder()
                .id(history.getId())
                .fieldChanged(history.getFieldChanged())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .changedByUsername(history.getChangedBy().getUsername())
                .changedAt(history.getCreatedAt())
                .build();
    }
}

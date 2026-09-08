package com.controlplane.backend.dto.task;

import com.controlplane.backend.dto.employee.EmployeeSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private UUID projectId;
    private String projectName;
    private EmployeeSummaryResponse assignee;
    private EmployeeSummaryResponse reporter;
    private LocalDate dueDate;
    private Instant completedAt;
    private Double estimatedHours;
    private long commentCount;
    private long attachmentCount;
    private Instant createdAt;
}

package com.controlplane.backend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryResponse {

    private UUID id;
    private String title;
    private String status;
    private String priority;
    private LocalDate dueDate;
    private String assigneeName;
}

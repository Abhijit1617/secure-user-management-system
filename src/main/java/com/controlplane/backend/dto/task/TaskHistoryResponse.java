package com.controlplane.backend.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistoryResponse {

    private UUID id;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private String changedByUsername;
    private Instant changedAt;
}

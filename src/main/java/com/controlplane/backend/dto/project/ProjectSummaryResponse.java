package com.controlplane.backend.dto.project;

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
public class ProjectSummaryResponse {

    private UUID id;
    private String name;
    private String code;
    private String status;
    private String priority;
    private LocalDate endDate;
}

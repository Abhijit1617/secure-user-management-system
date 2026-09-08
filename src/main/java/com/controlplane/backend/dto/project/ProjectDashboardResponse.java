package com.controlplane.backend.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardResponse {

    private long totalProjects;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countByPriority;
    private long overdueProjects;
    private java.util.List<ProjectSummaryResponse> upcomingDeadlines;
}

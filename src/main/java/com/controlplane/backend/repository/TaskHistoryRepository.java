package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Task;
import com.controlplane.backend.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {

    List<TaskHistory> findByTaskOrderByCreatedAtDesc(Task task);
}

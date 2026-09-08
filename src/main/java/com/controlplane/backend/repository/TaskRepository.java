package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Project;
import com.controlplane.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    long countByProject(Project project);
}

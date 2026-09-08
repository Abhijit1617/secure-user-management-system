package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.task.AssignTaskRequest;
import com.controlplane.backend.dto.task.CreateTaskRequest;
import com.controlplane.backend.dto.task.TaskHistoryResponse;
import com.controlplane.backend.dto.task.TaskResponse;
import com.controlplane.backend.dto.task.UpdateTaskDueDateRequest;
import com.controlplane.backend.dto.task.UpdateTaskPriorityRequest;
import com.controlplane.backend.dto.task.UpdateTaskRequest;
import com.controlplane.backend.dto.task.UpdateTaskStatusRequest;
import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Project;
import com.controlplane.backend.entity.Task;
import com.controlplane.backend.entity.TaskHistory;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.entity.enums.TaskStatus;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.TaskMapper;
import com.controlplane.backend.repository.AttachmentRepository;
import com.controlplane.backend.repository.CommentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.ProjectRepository;
import com.controlplane.backend.repository.TaskHistoryRepository;
import com.controlplane.backend.repository.TaskRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.repository.spec.TaskSpecifications;
import com.controlplane.backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.controlplane.backend.config.RedisCacheConfig.TASK_CACHE;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(UUID projectId, UUID assigneeId, TaskStatus status, String searchTerm,
                                              Pageable pageable) {
        Specification<Task> spec = TaskSpecifications.combine(
                TaskSpecifications.belongsToProject(projectId),
                TaskSpecifications.hasAssignee(assigneeId),
                TaskSpecifications.hasStatus(status),
                TaskSpecifications.searchTermMatches(searchTerm)
        );
        Page<Task> page = taskRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = TASK_CACHE, key = "#id")
    public TaskResponse getById(UUID id) {
        return toResponse(loadTask(id));
    }

    @Override
    @Transactional
    public TaskResponse create(CreateTaskRequest request, UUID requesterId) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Project", "id", request.getProjectId()));

        Employee assignee = request.getAssigneeId() != null
                ? loadEmployee(request.getAssigneeId()) : null;
        Employee reporter = request.getReporterId() != null
                ? loadEmployee(request.getReporterId()) : null;

        Task task = Task.builder()
                .project(project)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .assignee(assignee)
                .reporter(reporter)
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours())
                .build();

        task = taskRepository.save(task);

        if (assignee != null) {
            recordHistory(task, "ASSIGNEE", null, assignee.getEmployeeCode(), requesterId);
        }

        log.info("Created task [{}] in project [{}]", task.getTitle(), project.getCode());
        return toResponse(task);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = TASK_CACHE, key = "#id")
    public TaskResponse update(UUID id, UpdateTaskRequest request) {
        Task task = loadTask(id);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setEstimatedHours(request.getEstimatedHours());
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = TASK_CACHE, key = "#id")
    public TaskResponse assign(UUID id, AssignTaskRequest request, UUID requesterId) {
        Task task = loadTask(id);
        Employee newAssignee = loadEmployee(request.getAssigneeId());
        String previousAssignee = task.getAssignee() != null ? task.getAssignee().getEmployeeCode() : "unassigned";

        task.setAssignee(newAssignee);
        recordHistory(task, "ASSIGNEE", previousAssignee, newAssignee.getEmployeeCode(), requesterId);

        log.info("Task [{}] reassigned to [{}]", task.getTitle(), newAssignee.getEmployeeCode());
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = TASK_CACHE, key = "#id")
    public TaskResponse updateStatus(UUID id, UpdateTaskStatusRequest request, UUID requesterId) {
        Task task = loadTask(id);
        TaskStatus previousStatus = task.getStatus();

        task.setStatus(request.getStatus());
        if (request.getStatus() == TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        } else {
            task.setCompletedAt(null);
        }

        recordHistory(task, "STATUS", previousStatus.name(), request.getStatus().name(), requesterId);
        log.info("Task [{}] status changed from {} to {}", task.getTitle(), previousStatus, request.getStatus());
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = TASK_CACHE, key = "#id")
    public TaskResponse updatePriority(UUID id, UpdateTaskPriorityRequest request, UUID requesterId) {
        Task task = loadTask(id);
        String previousPriority = task.getPriority().name();

        task.setPriority(request.getPriority());
        recordHistory(task, "PRIORITY", previousPriority, request.getPriority().name(), requesterId);
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = TASK_CACHE, key = "#id")
    public TaskResponse updateDueDate(UUID id, UpdateTaskDueDateRequest request, UUID requesterId) {
        Task task = loadTask(id);
        String previousDueDate = task.getDueDate() != null ? task.getDueDate().toString() : "none";
        String newDueDate = request.getDueDate() != null ? request.getDueDate().toString() : "none";

        task.setDueDate(request.getDueDate());
        recordHistory(task, "DUE_DATE", previousDueDate, newDueDate, requesterId);
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> getHistory(UUID id) {
        Task task = loadTask(id);
        return taskHistoryRepository.findByTaskOrderByCreatedAtDesc(task).stream()
                .map(taskMapper::toHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = TASK_CACHE, key = "#id")
    public void delete(UUID id) {
        Task task = loadTask(id);
        taskRepository.delete(task);
        log.info("Deleted task [{}]", task.getTitle());
    }

    private void recordHistory(Task task, String field, String oldValue, String newValue, UUID requesterId) {
        User changedBy = userRepository.findById(requesterId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", requesterId));
        TaskHistory history = TaskHistory.builder()
                .task(task)
                .fieldChanged(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .build();
        taskHistoryRepository.save(history);
    }

    private TaskResponse toResponse(Task task) {
        long commentCount = commentRepository.countByOwnerTypeAndOwnerIdAndDeletedFalse(
                EntityReferenceType.TASK, task.getId());
        long attachmentCount = attachmentRepository.countByOwnerTypeAndOwnerId(
                EntityReferenceType.TASK, task.getId());
        return taskMapper.toResponse(task, commentCount, attachmentCount);
    }

    private Task loadTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Task", "id", id));
    }

    private Employee loadEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", "id", id));
    }
}

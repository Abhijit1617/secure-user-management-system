package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.task.UpdateTaskStatusRequest;
import com.controlplane.backend.entity.Project;
import com.controlplane.backend.entity.Task;
import com.controlplane.backend.entity.TaskHistory;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.entity.enums.Priority;
import com.controlplane.backend.entity.enums.TaskStatus;
import com.controlplane.backend.mapper.TaskMapper;
import com.controlplane.backend.repository.AttachmentRepository;
import com.controlplane.backend.repository.CommentRepository;
import com.controlplane.backend.repository.EmployeeRepository;
import com.controlplane.backend.repository.ProjectRepository;
import com.controlplane.backend.repository.TaskHistoryRepository;
import com.controlplane.backend.repository.TaskRepository;
import com.controlplane.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskHistoryRepository taskHistoryRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private UUID taskId;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        Project project = Project.builder().code("PRJ-1").build();
        project.setId(UUID.randomUUID());
        task = Task.builder()
                .project(project)
                .title("Fix login bug")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .build();
        task.setId(taskId);

        lenient().when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        lenient().when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        User requester = User.builder().username("jane.doe").build();
        requester.setId(requesterId);
        lenient().when(userRepository.findById(requesterId))
                .thenReturn(Optional.of(requester));
        lenient().when(commentRepository.countByOwnerTypeAndOwnerIdAndDeletedFalse(
                any(EntityReferenceType.class), any(UUID.class))).thenReturn(0L);
        lenient().when(attachmentRepository.countByOwnerTypeAndOwnerId(
                any(EntityReferenceType.class), any(UUID.class))).thenReturn(0L);
    }

    @Test
    void updateStatusToDoneSetsCompletedAt() {
        UpdateTaskStatusRequest request = UpdateTaskStatusRequest.builder().status(TaskStatus.DONE).build();

        taskService.updateStatus(taskId, request, requesterId);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void updateStatusRecordsHistoryEntry() {
        UpdateTaskStatusRequest request = UpdateTaskStatusRequest.builder().status(TaskStatus.IN_PROGRESS).build();

        taskService.updateStatus(taskId, request, requesterId);

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getFieldChanged()).isEqualTo("STATUS");
        assertThat(captor.getValue().getOldValue()).isEqualTo("TODO");
        assertThat(captor.getValue().getNewValue()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void movingAwayFromDoneClearsCompletedAt() {
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(java.time.Instant.now());

        UpdateTaskStatusRequest request = UpdateTaskStatusRequest.builder().status(TaskStatus.IN_REVIEW).build();
        taskService.updateStatus(taskId, request, requesterId);

        assertThat(task.getCompletedAt()).isNull();
    }
}

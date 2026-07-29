package com.controlplane.backend.service.impl;

import com.controlplane.backend.entity.NotificationPreference;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.NotificationType;
import com.controlplane.backend.mapper.NotificationMapper;
import com.controlplane.backend.repository.NotificationPreferenceRepository;
import com.controlplane.backend.repository.NotificationRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.EmailService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        recipient = User.builder().username("jane.doe").email("jane.doe@example.com")
                .firstName("Jane").build();
        recipient.setId(recipientId);
        lenient().when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
    }

    @Test
    void createFromEventPersistsNotificationWhenInAppEnabled() {
        NotificationPreference preference = NotificationPreference.builder()
                .user(recipient).emailEnabled(false).inAppEnabled(true).build();
        when(notificationPreferenceRepository.findByUser(recipient)).thenReturn(Optional.of(preference));

        notificationService.createFromEvent(recipientId, NotificationType.TASK_ASSIGNED,
                "Task assigned", "You were assigned a task", "TASK", UUID.randomUUID());

        verify(notificationRepository, times(1)).save(any());
        verify(emailService, never()).sendNotificationEmail(any(), any(), any(), any());
    }

    @Test
    void createFromEventSendsEmailWhenEmailEnabled() {
        NotificationPreference preference = NotificationPreference.builder()
                .user(recipient).emailEnabled(true).inAppEnabled(false).build();
        when(notificationPreferenceRepository.findByUser(recipient)).thenReturn(Optional.of(preference));

        notificationService.createFromEvent(recipientId, NotificationType.TASK_ASSIGNED,
                "Task assigned", "You were assigned a task", "TASK", UUID.randomUUID());

        verify(notificationRepository, never()).save(any());
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendNotificationEmail(
                org.mockito.ArgumentMatchers.eq("jane.doe@example.com"),
                org.mockito.ArgumentMatchers.eq("Jane"),
                subjectCaptor.capture(),
                org.mockito.ArgumentMatchers.anyString());
        assertThat(subjectCaptor.getValue()).isEqualTo("Task assigned");
    }

    @Test
    void createFromEventSkipsSilentlyForUnknownUser() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        notificationService.createFromEvent(unknownUserId, NotificationType.TASK_ASSIGNED,
                "Task assigned", "message", "TASK", UUID.randomUUID());

        verify(notificationRepository, never()).save(any());
    }
}

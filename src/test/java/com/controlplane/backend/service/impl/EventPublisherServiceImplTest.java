package com.controlplane.backend.service.impl;

import com.controlplane.backend.config.KafkaTopicConfig;
import com.controlplane.backend.entity.enums.AuditAction;
import com.controlplane.backend.entity.enums.NotificationType;
import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.event.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventPublisherServiceImpl eventPublisherService;

    @BeforeEach
    void setUp() {
        lenient().when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishesNotificationEventToTheCorrectTopicKeyedByRecipient() {
        UUID recipientId = UUID.randomUUID();
        NotificationEvent event = NotificationEvent.of(NotificationType.TASK_ASSIGNED, recipientId,
                "title", "message", "TASK", UUID.randomUUID());

        eventPublisherService.publishNotificationEvent(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.NOTIFICATION_EVENTS_TOPIC),
                eq(recipientId.toString()), eq(event));
    }

    @Test
    void publishesAuditEventToTheCorrectTopicKeyedByUser() {
        UUID userId = UUID.randomUUID();
        AuditEvent event = AuditEvent.of(userId, AuditAction.LOGIN, "User", userId, "127.0.0.1", null);

        eventPublisherService.publishAuditEvent(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.AUDIT_EVENTS_TOPIC), eq(userId.toString()), eq(event));
    }

    @Test
    void publishesSystemAuditEventWithSystemKeyWhenUserIsNull() {
        AuditEvent event = AuditEvent.of(null, AuditAction.LOGIN_FAILED, "User", null, "127.0.0.1", null);

        eventPublisherService.publishAuditEvent(event);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.AUDIT_EVENTS_TOPIC), eq("system"), eq(event));
    }
}

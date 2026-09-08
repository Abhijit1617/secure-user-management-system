package com.controlplane.backend.service.impl;

import com.controlplane.backend.config.KafkaTopicConfig;
import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.event.NotificationEvent;
import com.controlplane.backend.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherServiceImpl.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishNotificationEvent(NotificationEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_EVENTS_TOPIC, event.recipientUserId().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish notification event [{}]", event.eventId(), exception);
                    }
                });
    }

    @Override
    public void publishAuditEvent(AuditEvent event) {
        String key = event.userId() != null ? event.userId().toString() : "system";
        kafkaTemplate.send(KafkaTopicConfig.AUDIT_EVENTS_TOPIC, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish audit event [{}]", event.eventId(), exception);
                    }
                });
    }
}

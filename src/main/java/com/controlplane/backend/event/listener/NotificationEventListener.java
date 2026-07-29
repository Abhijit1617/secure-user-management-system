package com.controlplane.backend.event.listener;

import com.controlplane.backend.config.KafkaTopicConfig;
import com.controlplane.backend.event.NotificationEvent;
import com.controlplane.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link NotificationEvent} messages and turns them into a
 * persisted in-app notification and/or an email, depending on the
 * recipient's preferences. Failures are handled by the shared
 * {@code KafkaConsumerConfig} error handler (retry with backoff, then
 * dead-letter), so this listener itself stays a straightforward, throwing
 * consumer rather than needing its own try/catch plumbing.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopicConfig.NOTIFICATION_EVENTS_TOPIC, groupId = "control-plane-notifications")
    public void onNotificationEvent(NotificationEvent event) {
        log.debug("Consuming notification event [{}] for recipient [{}]", event.eventId(), event.recipientUserId());
        notificationService.createFromEvent(
                event.recipientUserId(),
                event.type(),
                event.title(),
                event.message(),
                event.referenceType(),
                event.referenceId()
        );
    }
}

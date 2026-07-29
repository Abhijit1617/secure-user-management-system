package com.controlplane.backend.event.listener;

import com.controlplane.backend.config.KafkaTopicConfig;
import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link AuditEvent} messages and appends them to {@code audit_logs}.
 * Publishing audit entries through Kafka rather than writing them
 * synchronously inside the originating service means a slow or briefly
 * unavailable audit write never blocks the request that triggered it, and
 * a burst of activity (e.g. bulk role changes) doesn't create write
 * contention on the audit_logs table during the request itself.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;

    @KafkaListener(topics = KafkaTopicConfig.AUDIT_EVENTS_TOPIC, groupId = "control-plane-audit")
    public void onAuditEvent(AuditEvent event) {
        log.debug("Consuming audit event [{}] action={} entity={}", event.eventId(), event.action(),
                event.entityName());
        auditService.recordEvent(event);
    }
}

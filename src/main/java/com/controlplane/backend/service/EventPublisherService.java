package com.controlplane.backend.service;

import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.event.NotificationEvent;

public interface EventPublisherService {

    void publishNotificationEvent(NotificationEvent event);

    void publishAuditEvent(AuditEvent event);
}

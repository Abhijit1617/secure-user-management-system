package com.controlplane.backend.event;

import com.controlplane.backend.entity.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Published whenever something happens that a user should be notified
 * about (task assigned, project status changed, account created). The
 * listener in {@code NotificationEventListener} turns this into a
 * persisted {@code Notification} row and, depending on preferences, an
 * email.
 */
public record NotificationEvent(
        UUID eventId,
        NotificationType type,
        UUID recipientUserId,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        Instant occurredAt
) {
    public static NotificationEvent of(NotificationType type, UUID recipientUserId, String title, String message,
                                        String referenceType, UUID referenceId) {
        return new NotificationEvent(UUID.randomUUID(), type, recipientUserId, title, message,
                referenceType, referenceId, Instant.now());
    }
}

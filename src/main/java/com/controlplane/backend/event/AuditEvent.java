package com.controlplane.backend.event;

import com.controlplane.backend.entity.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

/**
 * Published for every security-relevant or data-changing action. Kept as
 * an event rather than a direct repository write from the originating
 * service so that audit persistence never blocks or fails the request
 * that triggered it — {@code AuditEventListener} writes it asynchronously.
 */
public record AuditEvent(
        UUID eventId,
        UUID userId,
        AuditAction action,
        String entityName,
        UUID entityId,
        String oldValue,
        String newValue,
        String ipAddress,
        String userAgent,
        Instant occurredAt
) {
    public static AuditEvent of(UUID userId, AuditAction action, String entityName, UUID entityId,
                                 String ipAddress, String userAgent) {
        return new AuditEvent(UUID.randomUUID(), userId, action, entityName, entityId,
                null, null, ipAddress, userAgent, Instant.now());
    }
}

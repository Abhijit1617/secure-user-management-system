package com.controlplane.backend.service;

import com.controlplane.backend.dto.audit.AuditLogResponse;
import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.entity.enums.AuditAction;
import com.controlplane.backend.event.AuditEvent;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditService {

    /**
     * Persists an audit event consumed off Kafka. Not exposed over HTTP.
     */
    void recordEvent(AuditEvent event);

    PageResponse<AuditLogResponse> getEntityHistory(String entityName, UUID entityId, Pageable pageable);

    PageResponse<AuditLogResponse> getUserActivity(UUID userId, Pageable pageable);

    PageResponse<AuditLogResponse> search(AuditAction action, Pageable pageable);
}

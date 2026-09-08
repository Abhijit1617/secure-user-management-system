package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.audit.AuditLogResponse;
import com.controlplane.backend.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditMapper {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUser() != null ? auditLog.getUser().getId() : null)
                .username(auditLog.getUser() != null ? auditLog.getUser().getUsername() : "system")
                .action(auditLog.getAction().name())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}

package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.audit.AuditLogResponse;
import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.entity.AuditLog;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.AuditAction;
import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.mapper.AuditMapper;
import com.controlplane.backend.repository.AuditLogRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditMapper auditMapper;

    @Override
    @Transactional
    public void recordEvent(AuditEvent event) {
        User user = event.userId() != null ? userRepository.findById(event.userId()).orElse(null) : null;

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(event.action())
                .entityName(event.entityName())
                .entityId(event.entityId())
                .oldValue(event.oldValue())
                .newValue(event.newValue())
                .ipAddress(event.ipAddress())
                .userAgent(event.userAgent())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Recorded audit event [{}] action={} entity={}", event.eventId(), event.action(),
                event.entityName());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getEntityHistory(String entityName, UUID entityId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByEntityNameAndEntityId(entityName, entityId, pageable);
        return PageResponse.from(page.map(auditMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getUserActivity(UUID userId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(auditMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditAction action, Pageable pageable) {
        Page<AuditLog> page = action != null
                ? auditLogRepository.findByAction(action, pageable)
                : auditLogRepository.findAll(pageable);
        return PageResponse.from(page.map(auditMapper::toResponse));
    }
}

package com.controlplane.backend.controller;

import com.controlplane.backend.dto.audit.AuditLogResponse;
import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.entity.enums.AuditAction;
import com.controlplane.backend.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('AUDIT_READ')")
@Tag(name = "Audit Logs", description = "Entity change history, user activity, and security event search")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Search audit logs, optionally filtered by action, with pagination")
    public ResponseEntity<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(auditService.search(action, pageable));
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @Operation(summary = "Get the full change history for a specific entity instance",
            description = "e.g. /api/v1/audit-logs/entity/Task/{taskId} shows every recorded change to that task.")
    public ResponseEntity<PageResponse<AuditLogResponse>> getEntityHistory(
            @PathVariable String entityName,
            @PathVariable UUID entityId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(auditService.getEntityHistory(entityName, entityId, pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get everything a specific user has done, across every entity type")
    public ResponseEntity<PageResponse<AuditLogResponse>> getUserActivity(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(auditService.getUserActivity(userId, pageable));
    }
}

package com.controlplane.backend.controller;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.notification.NotificationPreferenceResponse;
import com.controlplane.backend.dto.notification.NotificationResponse;
import com.controlplane.backend.dto.notification.UpdateNotificationPreferenceRequest;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app notification history, unread count and delivery preferences")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get the authenticated user's notification history, newest first")
    public ResponseEntity<PageResponse<NotificationResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(notificationService.getHistory(principal.getId(), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the count of unread notifications")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.getId()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable UUID id) {
        notificationService.markAsRead(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get the authenticated user's notification delivery preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.getPreferences(principal.getId()));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update the authenticated user's notification delivery preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(notificationService.updatePreferences(principal.getId(), request));
    }
}

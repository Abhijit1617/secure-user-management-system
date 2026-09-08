package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.notification.NotificationPreferenceResponse;
import com.controlplane.backend.dto.notification.NotificationResponse;
import com.controlplane.backend.dto.notification.UpdateNotificationPreferenceRequest;
import com.controlplane.backend.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    PageResponse<NotificationResponse> getHistory(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    NotificationPreferenceResponse getPreferences(UUID userId);

    NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request);

    /**
     * Called by {@code NotificationEventListener} to persist an in-app
     * notification and, if the recipient's preferences allow it, trigger
     * an email. Not exposed over HTTP.
     */
    void createFromEvent(UUID recipientUserId, NotificationType type, String title, String message,
                          String referenceType, UUID referenceId);
}

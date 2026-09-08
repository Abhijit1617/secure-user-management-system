package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.notification.NotificationPreferenceResponse;
import com.controlplane.backend.dto.notification.NotificationResponse;
import com.controlplane.backend.entity.Notification;
import com.controlplane.backend.entity.NotificationPreference;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .emailEnabled(preference.isEmailEnabled())
                .inAppEnabled(preference.isInAppEnabled())
                .build();
    }
}

package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.notification.NotificationPreferenceResponse;
import com.controlplane.backend.dto.notification.NotificationResponse;
import com.controlplane.backend.dto.notification.UpdateNotificationPreferenceRequest;
import com.controlplane.backend.entity.Notification;
import com.controlplane.backend.entity.NotificationPreference;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.NotificationType;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.NotificationMapper;
import com.controlplane.backend.repository.NotificationPreferenceRepository;
import com.controlplane.backend.repository.NotificationRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.EmailService;
import com.controlplane.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getHistory(UUID userId, Pageable pageable) {
        User user = loadUser(userId);
        Page<Notification> page =
                notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);

        return PageResponse.from(page.map(notificationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        User user = loadUser(userId);
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        ResourceNotFoundException.of("Notification", "id", notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot mark another user's notification as read");
        }

        notification.setRead(true);
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = loadUser(userId);
        notificationRepository.markAllAsRead(user, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        User user = loadUser(userId);

        NotificationPreference preference =
                notificationPreferenceRepository.findByUser(user)
                        .orElseGet(() -> defaultPreference(user));

        return notificationMapper.toPreferenceResponse(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updatePreferences(
            UUID userId,
            UpdateNotificationPreferenceRequest request) {

        User user = loadUser(userId);

        NotificationPreference preference =
                notificationPreferenceRepository.findByUser(user)
                        .orElseGet(() ->
                                NotificationPreference.builder()
                                        .user(user)
                                        .build());

        preference.setEmailEnabled(request.isEmailEnabled());
        preference.setInAppEnabled(request.isInAppEnabled());

        return notificationMapper.toPreferenceResponse(
                notificationPreferenceRepository.save(preference));
    }

    @Override
    @Transactional
    public void createFromEvent(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId) {

        User recipient = userRepository.findById(recipientUserId).orElse(null);

        if (recipient == null) {
            log.warn("Skipping notification for unknown user [{}]", recipientUserId);
            return;
        }

        NotificationPreference preference =
                notificationPreferenceRepository.findByUser(recipient)
                        .orElseGet(() -> defaultPreference(recipient));

        if (preference.isInAppEnabled()) {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .title(title)
                    .message(message)
                    .type(type)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .build();

            notificationRepository.save(notification);
        }

        if (preference.isEmailEnabled()) {
            emailService.sendNotificationEmail(
                    recipient.getEmail(),
                    recipient.getFirstName(),
                    title,
                    message);
        }
    }

    private NotificationPreference defaultPreference(User user) {
        return notificationPreferenceRepository.save(
                NotificationPreference.builder()
                        .user(user)
                        .build());
    }

    private User loadUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        ResourceNotFoundException.of("User", "id", id));
    }
}
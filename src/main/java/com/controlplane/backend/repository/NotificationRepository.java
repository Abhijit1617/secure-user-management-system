package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Notification;
import com.controlplane.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, java.util.UUID> {

    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    long countByRecipientAndReadFalse(User recipient);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = CURRENT_TIMESTAMP "
            + "where n.recipient = :recipient and n.read = false")
    int markAllAsRead(@Param("recipient") User recipient);
}

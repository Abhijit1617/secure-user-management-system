package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Notification;
import com.controlplane.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientOrderByCreatedAtDesc(
            User recipient,
            Pageable pageable);

    long countByRecipientAndReadFalse(User recipient);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true,
                n.readAt = :readAt
            WHERE n.recipient = :recipient
              AND n.read = false
            """)
    int markAllAsRead(
            @Param("recipient") User recipient,
            @Param("readAt") Instant readAt);
}
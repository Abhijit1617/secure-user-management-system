package com.controlplane.backend.repository;

import com.controlplane.backend.entity.NotificationPreference;
import com.controlplane.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByUser(User user);
}

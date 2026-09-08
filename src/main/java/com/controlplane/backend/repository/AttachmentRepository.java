package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Attachment;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(EntityReferenceType ownerType, UUID ownerId);

    long countByOwnerTypeAndOwnerId(EntityReferenceType ownerType, UUID ownerId);
}

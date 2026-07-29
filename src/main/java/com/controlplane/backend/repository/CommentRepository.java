package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Comment;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByOwnerTypeAndOwnerIdAndParentCommentIsNullOrderByCreatedAtAsc(
            EntityReferenceType ownerType, UUID ownerId);

    List<Comment> findByParentCommentOrderByCreatedAtAsc(Comment parentComment);

    long countByOwnerTypeAndOwnerIdAndDeletedFalse(EntityReferenceType ownerType, UUID ownerId);
}

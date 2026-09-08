package com.controlplane.backend.repository;

import com.controlplane.backend.entity.Comment;
import com.controlplane.backend.entity.CommentRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRevisionRepository extends JpaRepository<CommentRevision, UUID> {

    List<CommentRevision> findByCommentOrderByCreatedAtDesc(Comment comment);
}

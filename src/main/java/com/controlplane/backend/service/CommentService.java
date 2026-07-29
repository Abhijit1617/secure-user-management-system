package com.controlplane.backend.service;

import com.controlplane.backend.dto.comment.CommentResponse;
import com.controlplane.backend.dto.comment.CommentRevisionResponse;
import com.controlplane.backend.dto.comment.CreateCommentRequest;
import com.controlplane.backend.dto.comment.UpdateCommentRequest;
import com.controlplane.backend.entity.enums.EntityReferenceType;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    List<CommentResponse> listThread(EntityReferenceType ownerType, UUID ownerId);

    CommentResponse create(CreateCommentRequest request, UUID authorId);

    CommentResponse update(UUID commentId, UpdateCommentRequest request, UUID requesterId, boolean requesterIsModerator);

    void delete(UUID commentId, UUID requesterId, boolean requesterIsModerator);

    List<CommentRevisionResponse> getEditHistory(UUID commentId);
}

package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.comment.CommentResponse;
import com.controlplane.backend.dto.comment.CommentRevisionResponse;
import com.controlplane.backend.entity.Comment;
import com.controlplane.backend.entity.CommentRevision;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentMapper {

    private static final String DELETED_PLACEHOLDER = "[this comment was deleted]";

    public CommentResponse toResponse(Comment comment, List<CommentResponse> replies) {
        boolean deleted = comment.isDeleted();
        return CommentResponse.builder()
                .id(comment.getId())
                .ownerType(comment.getOwnerType().name())
                .ownerId(comment.getOwnerId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .content(deleted ? DELETED_PLACEHOLDER : comment.getContent())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .mentionedUserIds(deleted ? java.util.Set.of() : comment.getMentionedUserIds())
                .edited(comment.isEdited())
                .editedAt(comment.getEditedAt())
                .deleted(deleted)
                .createdAt(comment.getCreatedAt())
                .replies(replies)
                .build();
    }

    public CommentRevisionResponse toRevisionResponse(CommentRevision revision) {
        return CommentRevisionResponse.builder()
                .id(revision.getId())
                .previousContent(revision.getPreviousContent())
                .editedByUsername(revision.getEditedBy().getUsername())
                .editedAt(revision.getCreatedAt())
                .build();
    }
}

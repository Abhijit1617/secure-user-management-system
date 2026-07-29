package com.controlplane.backend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private String ownerType;
    private UUID ownerId;
    private UUID authorId;
    private String authorName;
    private String content;
    private UUID parentCommentId;
    private Set<UUID> mentionedUserIds;
    private boolean edited;
    private Instant editedAt;
    private boolean deleted;
    private Instant createdAt;
    private List<CommentResponse> replies;
}

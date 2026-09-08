package com.controlplane.backend.entity;

import com.controlplane.backend.entity.enums.EntityReferenceType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A comment attached to a task or project, with optional threading
 * through parentComment.
 *
 * Deletion is soft so replies to a deleted comment remain intact.
 */
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(
                        name = "idx_comments_owner",
                        columnList = "owner_type, owner_id"
                ),
                @Index(
                        name = "idx_comments_parent",
                        columnList = "parent_comment_id"
                ),
                @Index(
                        name = "idx_comments_author",
                        columnList = "author_id"
                )
        }
)
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(
        exclude = {
                "author",
                "parentComment",
                "replies",
                "mentionedUserIds"
        }
)
public class Comment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30)
    private EntityReferenceType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Builder.Default
    @OneToMany(mappedBy = "parentComment")
    private Set<Comment> replies = new HashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "comment_mentions",
            joinColumns = @JoinColumn(name = "comment_id")
    )
    @Column(name = "mentioned_user_id")
    private Set<UUID> mentionedUserIds = new HashSet<>();

    @Column(name = "edited", nullable = false)
    @Builder.Default
    private boolean edited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
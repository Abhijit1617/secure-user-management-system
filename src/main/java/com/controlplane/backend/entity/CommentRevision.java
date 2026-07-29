package com.controlplane.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Snapshot of a comment's content immediately before an edit overwrote it.
 * {@link com.controlplane.backend.entity.BaseEntity#getCreatedAt()} on this
 * row is effectively "when this revision was superseded" — i.e. the edit
 * timestamp.
 */
@Entity
@Table(
        name = "comment_revisions",
        indexes = {
                @Index(name = "idx_comment_revisions_comment", columnList = "comment_id")
        }
)
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"comment", "editedBy"})
public class CommentRevision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Lob
    @Column(name = "previous_content", nullable = false, columnDefinition = "TEXT")
private String previousContent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_id", nullable = false)
    private User editedBy;
}

package com.controlplane.backend.entity;

import com.controlplane.backend.entity.enums.EntityReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

/**
 * A single uploaded file attached to an employee (document), a task, or a
 * project. The parent is referenced polymorphically via
 * {@link #ownerType}/{@link #ownerId} rather than a dedicated FK per parent
 * table, since one physical file store backs several unrelated modules.
 * The binary content itself lives on disk under
 * {@code app.storage.base-path}; this row is only the metadata.
 */
@Entity
@Table(
        name = "attachments",
        indexes = {
                @Index(name = "idx_attachments_owner", columnList = "owner_type, owner_id"),
                @Index(name = "idx_attachments_uploaded_by", columnList = "uploaded_by_id")
        }
)
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "uploadedBy")
public class Attachment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30)
    private EntityReferenceType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    /**
     * Path to the file on disk, relative to {@code app.storage.base-path}.
     * Never exposed to clients directly; downloads always go through the
     * download endpoint so access control is enforced on every request.
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;
}

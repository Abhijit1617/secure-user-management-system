package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.attachment.AttachmentResponse;
import com.controlplane.backend.entity.Attachment;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {

    public AttachmentResponse toResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .ownerType(attachment.getOwnerType().name())
                .ownerId(attachment.getOwnerId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSizeBytes(attachment.getFileSizeBytes())
                .description(attachment.getDescription())
                .uploadedByUsername(attachment.getUploadedBy() != null
                        ? attachment.getUploadedBy().getUsername() : null)
                .createdAt(attachment.getCreatedAt())
                .downloadUrl("/api/v1/attachments/" + attachment.getId() + "/download")
                .build();
    }
}

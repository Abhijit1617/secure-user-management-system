package com.controlplane.backend.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private UUID id;
    private String ownerType;
    private UUID ownerId;
    private String fileName;
    private String contentType;
    private long fileSizeBytes;
    private String description;
    private String uploadedByUsername;
    private Instant createdAt;
    private String downloadUrl;
}

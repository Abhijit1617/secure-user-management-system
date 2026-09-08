package com.controlplane.backend.service;

import com.controlplane.backend.dto.attachment.AttachmentResponse;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AttachmentService {

    List<AttachmentResponse> listFor(EntityReferenceType ownerType, UUID ownerId);

    AttachmentResponse upload(EntityReferenceType ownerType, UUID ownerId, MultipartFile file,
                               String description, UUID uploadedByUserId);

    AttachmentResponse getMetadata(UUID attachmentId);

    InputStreamResource download(UUID attachmentId);

    String getFileName(UUID attachmentId);

    String getContentType(UUID attachmentId);

    void delete(UUID attachmentId);
}

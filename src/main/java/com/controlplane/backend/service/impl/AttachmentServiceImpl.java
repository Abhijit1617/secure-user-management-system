package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.attachment.AttachmentResponse;
import com.controlplane.backend.entity.Attachment;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.AttachmentMapper;
import com.controlplane.backend.repository.AttachmentRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.AttachmentService;
import com.controlplane.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceImpl.class);

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listFor(EntityReferenceType ownerType, UUID ownerId) {
        return attachmentRepository.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType, ownerId).stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AttachmentResponse upload(EntityReferenceType ownerType, UUID ownerId, MultipartFile file,
                                      String description, UUID uploadedByUserId) {
        User uploadedBy = userRepository.findById(uploadedByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", uploadedByUserId));

        String subDirectory = ownerType.name().toLowerCase() + "/" + ownerId;
        String storagePath = fileStorageService.store(file, subDirectory);

        Attachment attachment = Attachment.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storagePath(storagePath)
                .description(description)
                .uploadedBy(uploadedBy)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("Uploaded attachment [{}] for {} [{}]", attachment.getFileName(), ownerType, ownerId);
        return attachmentMapper.toResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse getMetadata(UUID attachmentId) {
        return attachmentMapper.toResponse(loadAttachment(attachmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource download(UUID attachmentId) {
        Attachment attachment = loadAttachment(attachmentId);
        return new InputStreamResource(fileStorageService.retrieve(attachment.getStoragePath()));
    }

    @Override
    @Transactional(readOnly = true)
    public String getFileName(UUID attachmentId) {
        return loadAttachment(attachmentId).getFileName();
    }

    @Override
    @Transactional(readOnly = true)
    public String getContentType(UUID attachmentId) {
        return loadAttachment(attachmentId).getContentType();
    }

    @Override
    @Transactional
    public void delete(UUID attachmentId) {
        Attachment attachment = loadAttachment(attachmentId);
        fileStorageService.delete(attachment.getStoragePath());
        attachmentRepository.delete(attachment);
        log.info("Deleted attachment [{}]", attachment.getFileName());
    }

    private Attachment loadAttachment(UUID id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", "id", id));
    }
}

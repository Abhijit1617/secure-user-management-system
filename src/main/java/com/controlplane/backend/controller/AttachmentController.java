package com.controlplane.backend.controller;

import com.controlplane.backend.dto.attachment.AttachmentResponse;
import com.controlplane.backend.entity.enums.EntityReferenceType;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * File management endpoints. Uploads are scoped to an owning entity
 * (employee, task or project) via {@code ownerType}/{@code ownerId} so a
 * single upload/download/metadata surface serves every module that needs
 * attachments, rather than duplicating this controller per module.
 */
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "File Management", description = "Multipart upload, download and metadata for attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('ATTACHMENT_READ')")
    @Operation(summary = "List attachments for an owning entity (employee, task or project)")
    public ResponseEntity<List<AttachmentResponse>> list(@RequestParam EntityReferenceType ownerType,
                                                          @RequestParam UUID ownerId) {
        return ResponseEntity.ok(attachmentService.listFor(ownerType, ownerId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTACHMENT_READ')")
    @Operation(summary = "Get attachment metadata by id")
    public ResponseEntity<AttachmentResponse> getMetadata(@PathVariable UUID id) {
        return ResponseEntity.ok(attachmentService.getMetadata(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTACHMENT_UPLOAD')")
    @Operation(summary = "Upload a file (image, PDF, or document) attached to an employee, task or project",
            description = "Validated against app.storage.max-file-size-bytes and app.storage.allowed-content-types.")
    public ResponseEntity<AttachmentResponse> upload(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @RequestParam EntityReferenceType ownerType,
                                                      @RequestParam UUID ownerId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String description) {
        AttachmentResponse response = attachmentService.upload(ownerType, ownerId, file, description,
                principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('ATTACHMENT_READ')")
    @Operation(summary = "Download the raw bytes of an attachment")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        InputStreamResource resource = attachmentService.download(id);
        String fileName = attachmentService.getFileName(id);
        String contentType = attachmentService.getContentType(id);

        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTACHMENT_DELETE')")
    @Operation(summary = "Delete an attachment and its underlying file")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

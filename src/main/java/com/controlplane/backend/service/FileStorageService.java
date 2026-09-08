package com.controlplane.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Owns the physical storage of uploaded files on disk. Deliberately
 * separated from {@code AttachmentService} so the storage backend (local
 * disk today, S3 or Cloudinary tomorrow) can be swapped without touching
 * anything that deals with {@code Attachment} metadata or authorization.
 */
public interface FileStorageService {

    /**
     * Validates and persists the file to disk, returning the path it was
     * stored at, relative to {@code app.storage.base-path}.
     */
    String store(MultipartFile file, String subDirectory);

    InputStream retrieve(String relativeStoragePath);

    void delete(String relativeStoragePath);
}

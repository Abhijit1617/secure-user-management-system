package com.controlplane.backend.service.impl;

import com.controlplane.backend.config.StorageProperties;
import com.controlplane.backend.exception.InvalidFileException;
import com.controlplane.backend.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores files on the local filesystem under {@code app.storage.base-path}.
 * Suitable for a single-instance deployment or local development; a
 * production multi-instance deployment would swap this for an S3-backed
 * implementation behind the same {@link FileStorageService} interface.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final StorageProperties storageProperties;
    private final Path basePath;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.basePath = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to initialize storage directory: " + basePath, exception);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was provided");
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new InvalidFileException("File exceeds the maximum allowed size of "
                    + storageProperties.getMaxFileSizeBytes() + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !storageProperties.getAllowedContentTypes().contains(contentType)) {
            throw new InvalidFileException("File type '" + contentType + "' is not permitted");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalName.contains("..")) {
            throw new InvalidFileException("Invalid file name");
        }

        String storedFileName = UUID.randomUUID() + "-" + originalName;
        Path targetDirectory = basePath.resolve(subDirectory).normalize();
        if (!targetDirectory.startsWith(basePath)) {
            throw new InvalidFileException("Invalid storage path");
        }

        try {
            Files.createDirectories(targetDirectory);
            Path targetPath = targetDirectory.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = basePath.relativize(targetPath).toString().replace('\\', '/');
            log.info("Stored file [{}] ({} bytes)", relativePath, file.getSize());
            return relativePath;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to store uploaded file", exception);
        }
    }

    @Override
    public InputStream retrieve(String relativeStoragePath) {
        Path targetPath = basePath.resolve(relativeStoragePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new InvalidFileException("Invalid storage path");
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read stored file: " + relativeStoragePath, exception);
        }
    }

    @Override
    public void delete(String relativeStoragePath) {
        Path targetPath = basePath.resolve(relativeStoragePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new InvalidFileException("Invalid storage path");
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            log.warn("Failed to delete stored file: {}", relativeStoragePath, exception);
        }
    }
}

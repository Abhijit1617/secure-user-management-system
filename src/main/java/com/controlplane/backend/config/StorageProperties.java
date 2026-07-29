package com.controlplane.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {

    /**
     * Root directory files are stored under, relative or absolute. Every
     * attachment's {@code storagePath} is relative to this root.
     */
    private String basePath;

    private long maxFileSizeBytes;

    private List<String> allowedContentTypes;
}

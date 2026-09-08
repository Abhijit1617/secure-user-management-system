package com.controlplane.backend.config.health;

import com.controlplane.backend.config.StorageProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reports {@code DOWN} if the local file storage directory used by the
 * attachment module is missing or not writable, which would otherwise
 * surface only as a confusing 500 on the next upload attempt.
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageProperties storageProperties;

    public StorageHealthIndicator(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public Health health() {
        Path basePath = Paths.get(storageProperties.getBasePath()).toAbsolutePath().normalize();
        try {
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            Path probeFile = basePath.resolve(".health-check");
            Files.writeString(probeFile, "ok");
            Files.deleteIfExists(probeFile);

            return Health.up()
                    .withDetail("path", basePath.toString())
                    .withDetail("writable", true)
                    .build();
        } catch (IOException exception) {
            return Health.down()
                    .withDetail("path", basePath.toString())
                    .withDetail("error", exception.getMessage())
                    .build();
        }
    }
}

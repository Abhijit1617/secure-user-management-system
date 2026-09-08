package com.controlplane.backend.config.health;

import com.controlplane.backend.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageHealthIndicatorTest {

    @Test
    void reportsUpWhenDirectoryIsWritable(@org.junit.jupiter.api.io.TempDir Path tempDir) {
        StorageProperties properties = new StorageProperties();
        properties.setBasePath(tempDir.toString());

        StorageHealthIndicator indicator = new StorageHealthIndicator(properties);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("writable", true);
    }

    @Test
    void reportsDownWhenPathCannotBeCreated() throws Exception {
        Path tempDir = Files.createTempDirectory("storage-health-test");
        Path blockedPath = tempDir.resolve("blocked");
        Files.createFile(blockedPath);

        StorageProperties properties = new StorageProperties();
        properties.setBasePath(blockedPath.resolve("nested").toString());

        StorageHealthIndicator indicator = new StorageHealthIndicator(properties);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}

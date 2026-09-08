package com.controlplane.backend.config.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Reports Kafka broker connectivity by describing the cluster with a short
 * timeout, rather than relying on the next producer send or consumer poll
 * to reveal an outage. Spring Boot does not auto-configure a Kafka health
 * indicator the way it does for Redis and the primary DataSource, so this
 * fills that gap explicitly.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final int TIMEOUT_SECONDS = 3;

    private final String bootstrapServers;

    public KafkaHealthIndicator(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public Health health() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("request.timeout.ms", TIMEOUT_SECONDS * 1000);

        try (AdminClient adminClient = AdminClient.create(config)) {
            DescribeClusterResult result = adminClient.describeCluster();
            int nodeCount = result.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();
            String clusterId = result.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return Health.up()
                    .withDetail("bootstrapServers", bootstrapServers)
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();
        } catch (Exception exception) {
            return Health.down()
                    .withDetail("bootstrapServers", bootstrapServers)
                    .withDetail("error", exception.getMessage())
                    .build();
        }
    }
}

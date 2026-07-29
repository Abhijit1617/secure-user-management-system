package com.controlplane.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares every topic the application produces to or consumes from.
 * Spring Kafka's {@code KafkaAdmin} (auto-configured from
 * {@code spring.kafka.bootstrap-servers}) creates these on startup if they
 * don't already exist, which keeps local development and CI frictionless
 * without a separate topic-provisioning step.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";
    public static final String AUDIT_EVENTS_TOPIC = "audit-events";
    public static final String NOTIFICATION_EVENTS_DLT = "notification-events.DLT";
    public static final String AUDIT_EVENTS_DLT = "audit-events.DLT";

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(NOTIFICATION_EVENTS_TOPIC)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(AUDIT_EVENTS_TOPIC)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic notificationEventsDeadLetterTopic() {
        return TopicBuilder.name(NOTIFICATION_EVENTS_DLT)
                .partitions(1)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic auditEventsDeadLetterTopic() {
        return TopicBuilder.name(AUDIT_EVENTS_DLT)
                .partitions(1)
                .replicas(REPLICAS)
                .build();
    }
}

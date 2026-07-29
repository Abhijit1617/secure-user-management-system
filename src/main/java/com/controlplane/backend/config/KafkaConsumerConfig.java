package com.controlplane.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Every {@code @KafkaListener} in the application shares this error
 * handler. A failed message is retried a few times with exponential
 * backoff; if it still fails, {@link DeadLetterPublishingRecoverer}
 * republishes it to {@code <original-topic>.DLT} — its default naming
 * convention, which is why {@code KafkaTopicConfig} pre-declares topics
 * with exactly that suffix — rather than blocking the partition or
 * silently dropping the message.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    private static final long INITIAL_INTERVAL_MS = 1000L;
    private static final double MULTIPLIER = 2.0;
    private static final long MAX_INTERVAL_MS = 10_000L;
    private static final long MAX_ELAPSED_MS = 30_000L;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("kafkaTemplate") KafkaOperations<Object, Object> kafkaOperations) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations);

        ExponentialBackOff backOff = new ExponentialBackOff(INITIAL_INTERVAL_MS, MULTIPLIER);
        backOff.setMaxInterval(MAX_INTERVAL_MS);
        backOff.setMaxElapsedTime(MAX_ELAPSED_MS);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) ->
                log.warn("Retrying Kafka record from topic [{}], attempt {}: {}",
                        record.topic(), deliveryAttempt, exception.getMessage()));

        return errorHandler;
    }
}

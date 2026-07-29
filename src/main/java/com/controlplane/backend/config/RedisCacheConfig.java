package com.controlplane.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Configures Redis as the Spring Cache provider with a distinct TTL per
 * cache region, since a user record, an employee record and a dashboard
 * aggregate are stale on very different timescales.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String USER_CACHE = "users";
    public static final String EMPLOYEE_CACHE = "employees";
    public static final String PROJECT_CACHE = "projects";
    public static final String TASK_CACHE = "tasks";
    public static final String ORGANIZATION_CACHE = "organizations";
    public static final String DEPARTMENT_CACHE = "departments";
    public static final String DASHBOARD_CACHE = "dashboards";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = baseConfiguration(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                USER_CACHE, baseConfiguration(Duration.ofMinutes(15)),
                EMPLOYEE_CACHE, baseConfiguration(Duration.ofMinutes(15)),
                PROJECT_CACHE, baseConfiguration(Duration.ofMinutes(10)),
                TASK_CACHE, baseConfiguration(Duration.ofMinutes(5)),
                ORGANIZATION_CACHE, baseConfiguration(Duration.ofMinutes(30)),
                DEPARTMENT_CACHE, baseConfiguration(Duration.ofMinutes(20)),
                DASHBOARD_CACHE, baseConfiguration(Duration.ofMinutes(2))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    private RedisCacheConfiguration baseConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        jsonSerializer()));
    }

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}

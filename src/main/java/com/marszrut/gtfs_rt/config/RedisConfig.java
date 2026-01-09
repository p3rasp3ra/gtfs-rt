package com.marszrut.gtfs_rt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis.
 * Sets up the RedisTemplate with appropriate serializers for keys and values.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a RedisTemplate for VehiclePosition objects.
     * Uses String keys and JSON serialization for values.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, VehiclePosition> vehiclePositionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, VehiclePosition> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use StringRedisSerializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);

        // Create ObjectMapper with JavaTimeModule for Instant serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Use RedisSerializer.json() for values (stores as JSON)
        // This is the recommended approach in Spring Data Redis 4.0+
        // Create a custom JSON serializer using the ObjectMapper
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        template.setValueSerializer(jsonSerializer);

        // Set the same serializers for hash operations
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        return template;
    }
}

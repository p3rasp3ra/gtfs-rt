package com.marszrut.gtfs_rt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for Jackson ObjectMapper.
 * Creates and configures the ObjectMapper bean to be used throughout the application.
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates an ObjectMapper bean with customized settings.
     * Configures the ObjectMapper to handle Java 8 date/time types properly.
     *
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register JavaTimeModule to handle Java 8 date/time types (like Instant)
        objectMapper.registerModule(new JavaTimeModule());

        // Prevent timestamps from being written as timestamps (use ISO-8601 format instead)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}

package com.marszrut.gtfs_rt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

/**
 * Web configuration to support protobuf message conversion for REST endpoints.
 */
@Configuration
public class WebConfig {

    @Bean
    public ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }
}


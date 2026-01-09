package com.marszrut.gtfs_rt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String url;
    private String clientId;
    private String[] topics;
}


package com.marszrut.gtfs_rt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Custom Kafka deserializer for VehiclePosition.
 * Handles plain JSON messages.
 */
public class VehiclePositionDeserializer implements Deserializer<VehiclePosition> {

    private static final Logger logger = LoggerFactory.getLogger(VehiclePositionDeserializer.class);
    private final ObjectMapper objectMapper;

    public VehiclePositionDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        logger.info("VehiclePositionDeserializer initialized with provided ObjectMapper");
    }

    @Override
    public VehiclePosition deserialize(String topic, byte[] data) {
        if (data == null) {
            logger.warn("Received null data from topic: {}", topic);
            return null;
        }

        try {
            String jsonString = new String(data, StandardCharsets.UTF_8);
            logger.info("===== DESERIALIZER DEBUG: Raw JSON string: {} =====", jsonString);
            return objectMapper.readValue(jsonString, VehiclePosition.class);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing JSON into VehiclePosition", e);
        }
    }
}

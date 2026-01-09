package com.marszrut.gtfs_rt.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.exception.DataEnrichmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for deserializing JSON messages to domain objects.
 * This replaces the deprecated Spring Kafka JSON deserializers.
 */
@Service
public class JsonDeserializationService {

    private static final Logger logger = LoggerFactory.getLogger(JsonDeserializationService.class);
    private final ObjectMapper objectMapper;

    public JsonDeserializationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deserializes a JSON byte array to a VehiclePositionDomain object.
     *
     * @param jsonBytes the raw JSON bytes
     * @return the deserialized VehiclePositionDomain object
     * @throws DataEnrichmentException if deserialization fails
     */
    public VehiclePosition deserializeVehiclePosition(byte[] jsonBytes) {
        try {
            return objectMapper.readValue(jsonBytes, VehiclePosition.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize vehicle position JSON", e);
            throw new DataEnrichmentException("Failed to deserialize JSON to VehiclePositionDomain", e);
        }
    }
}

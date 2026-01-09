package com.marszrut.gtfs_rt.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.exception.DataEnrichmentException;
import com.marszrut.gtfs_rt.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Service responsible for validating GTFS-RT domain objects.
 * Performs business validation on domain objects and enriches them with additional data.
 */
@Service
public class EnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentService.class);
    private final ObjectMapper objectMapper;

    public EnrichmentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a Vehicle Position domain object.
     *
     * @param vehiclePosition the domain object to validate
     * @return validated VehiclePositionDomain object
     * @throws ValidationException if validation fails
     */
    @Transactional(readOnly = true)
    public VehiclePosition validate(VehiclePosition vehiclePosition) {
        validateVehiclePosition(vehiclePosition);

        // Set default timestamp if not provided
        if (vehiclePosition.getT() == null) {
            vehiclePosition.setT(Instant.now());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Validated vehicle position: vehicleId={}, lat={}, lon={}, timestamp={}",
                    vehiclePosition.getVid(),
                    vehiclePosition.getLat(),
                    vehiclePosition.getLon(),
                    vehiclePosition.getT());
        }

        return vehiclePosition;
    }

    /**
     * Enriches and validates a raw JSON Vehicle Position message.
     *
     * @param rawJson the raw JSON bytes from Kafka
     * @return enriched VehiclePositionDomain object
     * @throws DataEnrichmentException if deserialization fails
     * @throws ValidationException if validation fails
     */
    @Transactional(readOnly = true)
    public VehiclePosition enrichAndValidate(byte[] rawJson) {
        try {
            JsonNode json = objectMapper.readTree(rawJson);
            VehiclePosition vehiclePosition = mapToDomain(json);
            validateVehiclePosition(vehiclePosition);

            // Set default timestamp if not provided
            if (vehiclePosition.getT() == null) {
                vehiclePosition.setT(Instant.now());
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Enriched vehicle position: vehicleId={}, lat={}, lon={}, timestamp={}",
                        vehiclePosition.getVid(),
                        vehiclePosition.getLat(),
                        vehiclePosition.getLon(),
                        vehiclePosition.getT());
            }

            return vehiclePosition;
        } catch (IOException e) {
            logger.error("Failed to parse vehicle position JSON", e);
            throw new DataEnrichmentException("Failed to parse JSON payload", e);
        }
    }

    private VehiclePosition mapToDomain(JsonNode json) {
        VehiclePosition vehiclePosition = new VehiclePosition();

        // Extract vehicle information
        Optional.ofNullable(json.get("vehicle"))
                .ifPresent(vehicle -> {
                    vehiclePosition.setVid((vehicle.path("id").asText(null)));
                });

        // Extract position information
        Optional.ofNullable(json.get("position"))
                .ifPresent(position -> {
                    vehiclePosition.setLat(position.path("latitude").isDouble() ? position.get("latitude").asDouble() : null);
                    vehiclePosition.setLon(position.path("longitude").isDouble() ? position.get("longitude").asDouble() : null);
                });

        // Extract timestamp (Unix timestamp in seconds)
        if (json.has("timestamp") && json.get("timestamp").isNumber()) {
            long timestampSeconds = json.get("timestamp").asLong();
            vehiclePosition.setT(Instant.ofEpochSecond(timestampSeconds));
        } else {
            vehiclePosition.setT(Instant.now());
        }

        // Extract optional fields
        vehiclePosition.setAid(json.path("agencyId").asText(null));
        vehiclePosition.setRid(json.path("routeId").asText(null));
        vehiclePosition.setTid(json.path("tripId").asText(null));

        return vehiclePosition;
    }

    private void validateVehiclePosition(VehiclePosition vehiclePosition) {
        if (vehiclePosition.getVid() == null || vehiclePosition.getVid().isEmpty()) {
            throw new ValidationException("Vehicle ID is required");
        }
        if (vehiclePosition.getLat() == null || vehiclePosition.getLon() == null) {
            throw new ValidationException("Position coordinates are required");
        }
        if (vehiclePosition.getLat() < -90 || vehiclePosition.getLat() > 90) {
            throw new ValidationException("Invalid latitude: " + vehiclePosition.getLat());
        }
        if (vehiclePosition.getLon() < -180 || vehiclePosition.getLon() > 180) {
            throw new ValidationException("Invalid longitude: " + vehiclePosition.getLon());
        }
        if (vehiclePosition.getT() == null) {
            throw new ValidationException("Timestamp is required");
        }
    }
}

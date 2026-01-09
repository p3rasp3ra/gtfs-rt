package com.marszrut.gtfs_rt.ingestion;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.processing.StateUpdateService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for Vehicle Position messages - FAST PATH.
 * Receives fully deserialized VehiclePositionDomain objects directly from Kafka.
 * Processes messages for real-time state updates in Redis cache.
 */
@Component
public class JsonVpFastPathListener {

    private static final Logger logger = LoggerFactory.getLogger(JsonVpFastPathListener.class);
    private final StateUpdateService stateUpdateService;

    JsonVpFastPathListener(StateUpdateService stateUpdateService) {
        this.stateUpdateService = stateUpdateService;
    }

    @PostConstruct
    public void init() {
        logger.info("===== JsonVpFastPathListener initialized and ready to consume messages =====");
    }

    /**
     * Handles incoming vehicle position messages from Kafka for fast path processing.
     * Receives fully deserialized VehiclePositionDomain objects.
     * Updates Redis cache with current vehicle state.
     *
     * @param vehiclePosition the deserialized VehiclePositionDomain object
     * @param acknowledgment Kafka acknowledgment for manual offset commit
     */
    @KafkaListener(
        topics = "${kafka.topics.vehicle-positions}",
        groupId = "vp-json-fast-path-group",
        concurrency = "1",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVpFastPath(VehiclePosition vehiclePosition, Acknowledgment acknowledgment) {
        try {
            logger.info("===== [JSON FAST PATH] RECEIVED MESSAGE - vehicleId={}, lat={}, lon={}, timestamp={} =====",
                    vehiclePosition.getVid(),
                    vehiclePosition.getLat(),
                    vehiclePosition.getLon(),
                    vehiclePosition.getT());

            // Validate required fields before updating Redis
            if (vehiclePosition.getLat() == null) {
                logger.error("[JSON FAST PATH] VALIDATION FAILED: latitude is null. Skipping message. VehicleId={}",
                        vehiclePosition.getVid());
                acknowledgment.acknowledge(); // Skip this message
                return;
            }

            if (vehiclePosition.getLon() == null) {
                logger.error("[JSON FAST PATH] VALIDATION FAILED: longitude is null. Skipping message. VehicleId={}",
                        vehiclePosition.getVid());
                acknowledgment.acknowledge(); // Skip this message
                return;
            }

            // Fast path - Update current state in Redis
            stateUpdateService.updateCurrentState(vehiclePosition);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            logger.info("[JSON FAST PATH] Updated Redis cache: vehicleId={}, lat={}, lon={}",
                    vehiclePosition.getVid(), vehiclePosition.getLat(), vehiclePosition.getLon());

        } catch (Exception e) {
            logger.error("[JSON FAST PATH] Failed to process vehicle position: {}", e.getMessage(), e);
            // Don't acknowledge - message will be retried
        }
    }
}

package com.marszrut.gtfs_rt.ingestion;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.processing.HistoryWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for Vehicle Position messages - SLOW PATH.
 * Receives fully deserialized VehiclePositionDomain objects directly from Kafka.
 * Processes messages for historical persistence to TimescaleDB.
 */
@Component
public class JsonVpSlowPathListener {

    private static final Logger logger = LoggerFactory.getLogger(JsonVpSlowPathListener.class);
    private final HistoryWriter historyWriter;

    JsonVpSlowPathListener(HistoryWriter historyWriter) {
        this.historyWriter = historyWriter;
    }

    @PostConstruct
    public void init() {
        logger.info("===== JsonVpSlowPathListener initialized and ready to consume messages =====");
    }

    /**
     * Handles incoming vehicle position messages from Kafka for slow path processing.
     * Receives fully deserialized VehiclePositionDomain objects.
     * Persists data to TimescaleDB for historical record.
     *
     * @param vehiclePosition the deserialized VehiclePositionDomain object
     * @param acknowledgment Kafka acknowledgment for manual offset commit
     */
    @KafkaListener(
            topics = "${kafka.topics.vehicle-positions}",
            groupId = "vp-json-slow-path-group",
            concurrency = "1",
            containerFactory = "slowPathKafkaListenerContainerFactory"
    )
    public void handleVpSlowPath(VehiclePosition vehiclePosition, Acknowledgment acknowledgment) {
        try {
            logger.info("===== [JSON SLOW PATH] RECEIVED MESSAGE - vehicleId={}, lat={}, lon={}, timestamp={} =====",
                    vehiclePosition.getVid(),
                    vehiclePosition.getLat(),
                    vehiclePosition.getLon(),
                    vehiclePosition.getT());

            // Slow path - Persist to TimescaleDB for historical record
            historyWriter.persistHistory(vehiclePosition);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            logger.info("[JSON SLOW PATH] Successfully persisted to database: vehicleId={}, lat={}, lon={}, timestamp={}",
                    vehiclePosition.getVid(),
                    vehiclePosition.getLat(),
                    vehiclePosition.getLon(),
                    vehiclePosition.getT());

        } catch (Exception e) {
            logger.error("[JSON SLOW PATH] Failed to persist vehicle position: {}", e.getMessage(), e);
            // Spring Kafka's error handler will now forward this to the DLQ
            throw e;
        }
    }
}

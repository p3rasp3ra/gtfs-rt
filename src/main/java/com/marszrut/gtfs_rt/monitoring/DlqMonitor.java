package com.marszrut.gtfs_rt.monitoring;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Monitors Dead Letter Queue (DLQ) topics for failed messages.
 * Logs failures and can be extended to send alerts (Slack, email, PagerDuty, etc.)
 */
@Component
public class DlqMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DlqMonitor.class);

    @PostConstruct
    public void init() {
        logger.info("===== DLQ Monitor initialized - watching for failed messages =====");
    }

    /**
     * Monitors the slow-path DLQ for database persistence failures.
     * These failures typically indicate:
     * - Database schema mismatches
     * - Database connection issues
     * - Data validation failures
     * - Constraint violations
     */
    @KafkaListener(
            topics = "${kafka.topics.vehicle-positions.slow-path-dlq}",
            groupId = "dlq-monitor-slow-path",
            autoStartup = "true"
    )
    public void monitorSlowPathDlq(VehiclePosition vp) {
        logger.error("========================================");
        logger.error("SLOW PATH DLQ - Database persistence failure detected!");
        logger.error("VehicleId: {}", vp.getVid());
        logger.error("Latitude: {}, Longitude: {}", vp.getLat(), vp.getLon());
        logger.error("Timestamp: {}", vp.getT());
        logger.error("Trip ID: {}", vp.getTid());
        logger.error("Route ID: {}", vp.getRid());
        logger.error("========================================");
        logger.error("Possible causes:");
        logger.error("  1. Database schema mismatch (missing columns)");
        logger.error("  2. Database connection failure");
        logger.error("  3. Data constraint violations");
        logger.error("  4. Transaction timeout");
        logger.error("Action required: Check database schema and logs");
        logger.error("========================================");

        // TODO: Send alert to monitoring system (Slack, PagerDuty, email, etc.)
        // Example: slackNotificationService.sendAlert("Slow-path DLQ failure", vp);
    }

    /**
     * Monitors the fast-path DLQ for in-memory processing failures.
     * These failures typically indicate:
     * - Redis connection issues
     * - Cache update failures
     * - WebSocket broadcast failures
     * - Data validation issues
     */
    @KafkaListener(
            topics = "${kafka.topics.vehicle-positions.fast-path-dlq}",
            groupId = "dlq-monitor-fast-path",
            autoStartup = "true"
    )
    public void monitorFastPathDlq(VehiclePosition vp) {
        logger.error("========================================");
        logger.error("FAST PATH DLQ - In-memory processing failure detected!");
        logger.error("VehicleId: {}", vp.getVid());
        logger.error("Latitude: {}, Longitude: {}", vp.getLat(), vp.getLon());
        logger.error("Timestamp: {}", vp.getT());
        logger.error("========================================");
        logger.error("Possible causes:");
        logger.error("  1. Redis connection failure");
        logger.error("  2. Cache update error");
        logger.error("  3. WebSocket broadcast failure");
        logger.error("  4. Data validation error");
        logger.error("Action required: Check Redis and application logs");
        logger.error("========================================");

        // TODO: Send alert to monitoring system
        // Example: slackNotificationService.sendAlert("Fast-path DLQ failure", vp);
    }
}


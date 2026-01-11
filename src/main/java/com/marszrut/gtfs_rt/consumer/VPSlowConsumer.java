package com.marszrut.gtfs_rt.consumer;

import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.converter.VPConverter;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.repository.VPRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Slow consumer - reads protobuf from Kafka and persists to TimescaleDB.
 * Purpose: Store full vehicle position history for analysis.
 * Uses manual acknowledgment with retry logic for transient errors.
 */
@Service
public class VPSlowConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VPSlowConsumer.class);

    private final VPConverter converter;
    private final VPRepository repository;

    public VPSlowConsumer(VPConverter converter, VPRepository repository) {
        this.converter = converter;
        this.repository = repository;
    }

    @KafkaListener(
        topics = "${kafka.topics.vehicle-positions-proto}",
        groupId = "${kafka.consumer.group-id-slow}",
        containerFactory = "protoKafkaListenerContainerFactory"
    )
    public void processVehiclePosition(ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        try {
            // Extract metadata from headers with null checks
            Header feedIdHeader = record.headers().lastHeader("feedId");
            Header agencyIdHeader = record.headers().lastHeader("agencyId");

            if (feedIdHeader == null || agencyIdHeader == null) {
                logger.error("Missing required headers (feedId or agencyId) for message: key={}, partition={}, offset={}",
                            record.key(), record.partition(), record.offset());
                ack.acknowledge(); // Acknowledge to skip bad message
                return;
            }

            String feedId = new String(feedIdHeader.value(), StandardCharsets.UTF_8);
            String agencyId = new String(agencyIdHeader.value(), StandardCharsets.UTF_8);

            // Deserialize protobuf
            GtfsRealtime.FeedEntity feedEntity = GtfsRealtime.FeedEntity.parseFrom(record.value());

            // Convert to domain object
            VehiclePosition vp = converter.mapFromFeedEntity(feedEntity, feedId, agencyId);

            if (vp == null) {
                logger.warn("FeedEntity does not contain vehicle position: key={}, feedId={}, agencyId={}, offset={}",
                           record.key(), feedId, agencyId, record.offset());
                ack.acknowledge(); // Acknowledge to skip
                return;
            }

            // Save to TimescaleDB
            repository.save(vp);

            logger.debug("Saved VP to TimescaleDB: vehicleId={}, feedId={}, agencyId={}, partition={}, offset={}",
                        vp.getVid(), feedId, agencyId, record.partition(), record.offset());

            ack.acknowledge();

        } catch (DataIntegrityViolationException e) {
            // Data constraint violation - likely duplicate or invalid data
            logger.error("Data integrity violation - skipping message: key={}, partition={}, offset={}, error={}",
                        record.key(), record.partition(), record.offset(), e.getMessage());
            ack.acknowledge(); // Acknowledge to skip bad data

        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            // Invalid protobuf - non-retryable
            logger.error("Invalid protobuf message - skipping: key={}, partition={}, offset={}, error={}",
                        record.key(), record.partition(), record.offset(), e.getMessage());
            ack.acknowledge(); // Acknowledge to skip bad message

        } catch (Exception e) {
            // Transient errors (DB connection, etc.) - should retry
            logger.error("Slow consumer failed to process message (will retry): key={}, partition={}, offset={}, error={}",
                        record.key(), record.partition(), record.offset(), e.getMessage(), e);
            // Don't acknowledge - will retry via error handler
        }
    }
}


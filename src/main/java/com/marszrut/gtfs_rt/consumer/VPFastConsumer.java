package com.marszrut.gtfs_rt.consumer;

import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.converter.VPConverter;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Fast consumer - reads protobuf from Kafka and caches in Redis.
 * Purpose: Provide latest vehicle position for fast feed aggregation.
 * Uses manual acknowledgment for reliability.
 */
@Service
public class VPFastConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VPFastConsumer.class);

    private final VPConverter converter;
    private final RedisTemplate<String, VehiclePosition> redisTemplate;
    private final int cacheTtlSeconds;

    public VPFastConsumer(
            VPConverter converter,
            RedisTemplate<String, VehiclePosition> redisTemplate,
            @Value("${gtfs.feed.cache.ttl-seconds}") int cacheTtlSeconds) {
        this.converter = converter;
        this.redisTemplate = redisTemplate;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    @KafkaListener(
        topics = "${kafka.topics.vehicle-positions-proto}",
        groupId = "${kafka.consumer.group-id-fast}",
        containerFactory = "protoKafkaListenerContainerFactory"
    )
    public void processVehiclePosition(ConsumerRecord<String, byte[]> record, Acknowledgment ack) {
        try {
            // Extract metadata from headers with null checks
            Header feedIdHeader = record.headers().lastHeader("feedId");
            Header agencyIdHeader = record.headers().lastHeader("agencyId");

            if (feedIdHeader == null || agencyIdHeader == null) {
                logger.error("Missing required headers (feedId or agencyId) for message: key={}, offset={}",
                            record.key(), record.offset());
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
                logger.warn("FeedEntity does not contain vehicle position: key={}, feedId={}, agencyId={}",
                           record.key(), feedId, agencyId);
                ack.acknowledge(); // Acknowledge to skip
                return;
            }

            // Cache in Redis
            String redisKey = "vp:" + vp.getVid();
            redisTemplate.opsForValue().set(redisKey, vp, cacheTtlSeconds * 2L, TimeUnit.SECONDS);

            logger.debug("Cached VP in Redis: vehicleId={}, feedId={}, agencyId={}, partition={}, offset={}",
                        vp.getVid(), feedId, agencyId, record.partition(), record.offset());

            ack.acknowledge();

        } catch (Exception e) {
            logger.error("Fast consumer failed to process message: key={}, partition={}, offset={}, error={}",
                        record.key(), record.partition(), record.offset(), e.getMessage(), e);
            // Don't acknowledge - will retry (via error handler)
        }
    }
}


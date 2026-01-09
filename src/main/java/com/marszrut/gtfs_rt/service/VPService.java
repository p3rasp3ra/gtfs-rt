package com.marszrut.gtfs_rt.service;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VPService {

    private static final Logger logger = LoggerFactory.getLogger(VPService.class);

    private final KafkaTemplate<String, VehiclePosition> kafkaTemplate;
    private final RedisTemplate<String, VehiclePosition> redisTemplate;
    private final String topic;
    private final int cacheTtlSeconds;

    public VPService(KafkaTemplate<String, VehiclePosition> kafkaTemplate,
                     RedisTemplate<String, VehiclePosition> redisTemplate,
                     @Value("${kafka.topics.vehicle-positions}") String topic,
                     @Value("${gtfs.feed.cache.ttl-seconds}") int cacheTtlSeconds) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.topic = topic;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    /**
     * Sends vehicle position to Kafka and caches in Redis.
     * Redis cache enables fast feed aggregation for transit consumers.
     *
     * @param vp vehicle position to send
     */
    public void sendToKafka(VehiclePosition vp) {
        // Send to Kafka
        kafkaTemplate.send(topic, vp.getVid(), vp);
        logger.debug("Sent VehiclePosition to Kafka topic {} with key {}", topic, vp.getVid());

        // Cache in Redis for feed aggregation
        cacheInRedis(vp);
    }

    /**
     * Caches vehicle position in Redis with TTL.
     * Key pattern: "vp:{vehicleId}"
     *
     * @param vp vehicle position to cache
     */
    private void cacheInRedis(VehiclePosition vp) {
        try {
            String redisKey = "vp:" + vp.getVid();
            redisTemplate.opsForValue().set(redisKey, vp, cacheTtlSeconds * 2L, TimeUnit.SECONDS);
            logger.debug("Cached VehiclePosition in Redis with key: {}", redisKey);
        } catch (Exception e) {
            logger.error("Failed to cache vehicle position in Redis: vehicleId={}, error={}",
                        vp.getVid(), e.getMessage());
        }
    }
}

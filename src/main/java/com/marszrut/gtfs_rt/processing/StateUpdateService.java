package com.marszrut.gtfs_rt.processing;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service responsible for updating the current state of vehicle positions in Redis.
 * This service maintains the real-time cache of vehicle positions.
 */
@Service
public class StateUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(StateUpdateService.class);
    private static final String VEHICLE_POSITION_KEY_PREFIX = "vp:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30); // Time-to-live for cache entries

    private final RedisTemplate<String, VehiclePosition> redisTemplate;

    public StateUpdateService(RedisTemplate<String, VehiclePosition> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Updates the current state of a vehicle position in Redis.
     * The key is constructed using the vehicle ID and the value is the domain object.
     *
     * @param vehiclePosition the validated vehicle position domain object
     */
    public void updateCurrentState(VehiclePosition vehiclePosition) {
        String key = buildRedisKey(vehiclePosition);

        try {
            redisTemplate.opsForValue().set(key, vehiclePosition, CACHE_TTL);
            logger.debug("Updated state for vehicle: {}", vehiclePosition.getVid());
        } catch (Exception e) {
            logger.error("Failed to update Redis state for vehicle: {}", vehiclePosition.getVid(), e);
            throw new RuntimeException("Failed to update vehicle position state in Redis", e);
        }
    }

    /**
     * Builds a Redis key for a vehicle position.
     * The format is "vp:{agencyId}:{vehicleId}" if agencyId is available,
     * otherwise "vp:{vehicleId}".
     *
     * @param vehiclePosition the vehicle position domain object
     * @return the Redis key
     */
    private String buildRedisKey(VehiclePosition vehiclePosition) {
        StringBuilder keyBuilder = new StringBuilder(VEHICLE_POSITION_KEY_PREFIX);

        if (vehiclePosition.getAid() != null && !vehiclePosition.getAid().isEmpty()) {
            keyBuilder.append(vehiclePosition.getAid()).append(":");
        }

        keyBuilder.append(vehiclePosition.getVid());
        return keyBuilder.toString();
    }
}

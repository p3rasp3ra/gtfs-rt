package com.marszrut.gtfs_rt.service;

import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.converter.VPConverter;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for aggregating GTFS-RT feed data from Redis cache.
 * Builds complete FeedMessage for transit consumers.
 */
@Service
public class FeedService {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);

    private final RedisTemplate<String, VehiclePosition> redisTemplate;
    private final VPConverter converter;
    private final String feedVersion;
    private final String feedIncrementality;

    public FeedService(RedisTemplate<String, VehiclePosition> redisTemplate,
                       VPConverter converter,
                       @Value("${gtfs.feed.version}") String feedVersion,
                       @Value("${gtfs.feed.incrementality}") String feedIncrementality) {
        this.redisTemplate = redisTemplate;
        this.converter = converter;
        this.feedVersion = feedVersion;
        this.feedIncrementality = feedIncrementality;
    }

    /**
     * Builds a complete GTFS-RT FeedMessage with all vehicle positions from Redis.
     *
     * @param feedId optional feed ID filter
     * @param agencyId optional agency ID filter
     * @return complete FeedMessage with header and entities
     */
    public GtfsRealtime.FeedMessage buildVehiclePositionFeed(String feedId, String agencyId) {
        logger.debug("Building GTFS-RT feed for feedId={}, agencyId={}", feedId, agencyId);

        // Build feed header
        GtfsRealtime.FeedHeader.Builder headerBuilder = GtfsRealtime.FeedHeader.newBuilder()
            .setGtfsRealtimeVersion(feedVersion)
            .setTimestamp(Instant.now().getEpochSecond());

        // Set incrementality
        if ("FULL_DATASET".equals(feedIncrementality)) {
            headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET);
        } else if ("DIFFERENTIAL".equals(feedIncrementality)) {
            headerBuilder.setIncrementality(GtfsRealtime.FeedHeader.Incrementality.DIFFERENTIAL);
        }

        // Retrieve all vehicle positions from Redis
        List<GtfsRealtime.FeedEntity> entities = getVehiclePositionsFromRedis(feedId, agencyId);

        logger.info("Built GTFS-RT feed with {} vehicle position entities", entities.size());

        // Build and return complete feed message
        return GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(headerBuilder.build())
            .addAllEntity(entities)
            .build();
    }

    /**
     * Retrieves vehicle positions from Redis and converts to FeedEntity list.
     *
     * @param feedId optional feed ID filter
     * @param agencyId optional agency ID filter
     * @return list of FeedEntity objects
     */
    private List<GtfsRealtime.FeedEntity> getVehiclePositionsFromRedis(String feedId, String agencyId) {
        List<GtfsRealtime.FeedEntity> entities = new ArrayList<>();

        // Get all keys matching the pattern "vp:*"
        Set<String> keys = redisTemplate.keys("vp:*");

        if (keys == null || keys.isEmpty()) {
            logger.warn("No vehicle positions found in Redis");
            return entities;
        }

        logger.debug("Found {} vehicle position keys in Redis", keys.size());

        // Retrieve each vehicle position and convert to FeedEntity
        for (String key : keys) {
            try {
                VehiclePosition vp = redisTemplate.opsForValue().get(key);

                if (vp != null) {
                    // Apply filters if provided
                    if (feedId != null && !feedId.isEmpty() && !feedId.equals(vp.getFid())) {
                        continue;
                    }
                    if (agencyId != null && !agencyId.isEmpty() && !agencyId.equals(vp.getAid())) {
                        continue;
                    }

                    // Convert to FeedEntity and add to list
                    GtfsRealtime.FeedEntity entity = converter.entityToFeedEntity(vp);
                    entities.add(entity);
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve or convert vehicle position for key {}: {}", key, e.getMessage());
            }
        }

        return entities;
    }

    /**
     * Gets the timestamp of the most recent vehicle position update.
     *
     * @return epoch timestamp of most recent update, or current time if none found
     */
    public long getLastModifiedTimestamp() {
        Set<String> keys = redisTemplate.keys("vp:*");

        if (keys == null || keys.isEmpty()) {
            return Instant.now().getEpochSecond();
        }

        long maxTimestamp = 0;
        for (String key : keys) {
            VehiclePosition vp = redisTemplate.opsForValue().get(key);
            if (vp != null && vp.getT() != null) {
                long timestamp = vp.getT().getEpochSecond();
                if (timestamp > maxTimestamp) {
                    maxTimestamp = timestamp;
                }
            }
        }

        return maxTimestamp > 0 ? maxTimestamp : Instant.now().getEpochSecond();
    }
}


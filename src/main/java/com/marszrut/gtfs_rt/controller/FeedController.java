package com.marszrut.gtfs_rt.controller;

import com.google.protobuf.TextFormat;
import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Controller for serving aggregated GTFS-RT feed to transit consumers.
 * Supports both binary protobuf (production) and ASCII text format (development/debug).
 */
@RestController
@RequestMapping("/gtfs-rt")
public class FeedController {

    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);
    private static final String PROTOBUF_CONTENT_TYPE = "application/x-protobuf";
    private static final String TEXT_CONTENT_TYPE = "text/plain";

    private final FeedService feedService;
    private final int cacheTtlSeconds;

    public FeedController(FeedService feedService,
                          @org.springframework.beans.factory.annotation.Value("${gtfs.feed.cache.ttl-seconds}") int cacheTtlSeconds) {
        this.feedService = feedService;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    /**
     * Serves aggregated GTFS-RT feed with vehicle positions.
     * Supports both binary protobuf and ASCII text format based on Accept header.
     * - Accept: application/x-protobuf → binary format (production)
     * - Accept: text/plain → ASCII text format (development/debug)
     * Supports HTTP 304 Not Modified for efficient caching.
     *
     * @param feedId optional feed ID filter
     * @param agencyId optional agency ID filter
     * @param ifModifiedSince HTTP If-Modified-Since header for conditional GET
     * @param accept HTTP Accept header to determine response format
     * @return GTFS-RT FeedMessage in requested format
     */
    @GetMapping(value = "/feed.pb", produces = {PROTOBUF_CONTENT_TYPE, TEXT_CONTENT_TYPE})
    public ResponseEntity<byte[]> getVehiclePositionFeed(
            @RequestParam(required = false) String feedId,
            @RequestParam(required = false) String agencyId,
            @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Long ifModifiedSince,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = PROTOBUF_CONTENT_TYPE) String accept) {

        logger.info("GTFS-RT feed requested: feedId={}, agencyId={}, ifModifiedSince={}, accept={}",
                     feedId, agencyId, ifModifiedSince, accept);

        // Get last modified timestamp from Redis
        long lastModified = feedService.getLastModifiedTimestamp();

        // Check if client has current data (HTTP 304 Not Modified)
        if (ifModifiedSince != null && lastModified <= ifModifiedSince) {
            logger.info("Returning 304 Not Modified - client has current data");
            return ResponseEntity
                .status(HttpStatus.NOT_MODIFIED)
                .lastModified(Instant.ofEpochSecond(lastModified))
                .cacheControl(CacheControl.maxAge(cacheTtlSeconds, TimeUnit.SECONDS))
                .build();
        }

        // Build complete feed message
        GtfsRealtime.FeedMessage feedMessage = feedService.buildVehiclePositionFeed(feedId, agencyId);

        // Determine response format based on Accept header
        boolean isTextFormat = accept != null && accept.contains("text/plain");
        byte[] feedData;
        String contentType;

        if (isTextFormat) {
            // ASCII text format for debugging
            String textFormat = TextFormat.printer()
                .printToString(feedMessage);
            feedData = textFormat.getBytes(StandardCharsets.UTF_8);
            contentType = TEXT_CONTENT_TYPE + ";charset=UTF-8";
            logger.info("Serving GTFS-RT feed in ASCII text format with {} entities, {} bytes",
                        feedMessage.getEntityCount(), feedData.length);
        } else {
            // Binary protobuf format for production
            feedData = feedMessage.toByteArray();
            contentType = PROTOBUF_CONTENT_TYPE;
            logger.info("Serving GTFS-RT feed in binary protobuf format with {} entities, {} bytes",
                        feedMessage.getEntityCount(), feedData.length);
        }

        // Return with proper caching headers
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(contentType))
            .lastModified(Instant.ofEpochSecond(lastModified))
            .cacheControl(CacheControl.maxAge(cacheTtlSeconds, TimeUnit.SECONDS))
            .body(feedData);
    }

    /**
     * Health check endpoint for feed availability.
     *
     * @return simple status message
     */
    @GetMapping("/status")
    public ResponseEntity<String> getFeedStatus() {
        long lastModified = feedService.getLastModifiedTimestamp();
        long ageSeconds = Instant.now().getEpochSecond() - lastModified;

        return ResponseEntity.ok(
            String.format("GTFS-RT feed active. Last update: %d seconds ago", ageSeconds)
        );
    }
}


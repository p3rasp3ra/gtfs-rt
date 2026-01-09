package com.marszrut.gtfs_rt.controller;

import com.google.protobuf.TextFormat;
import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.converter.VPConverter;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.dto.VPDto;
import com.marszrut.gtfs_rt.service.VPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/vp")
public class VPController {

    private static final Logger logger = LoggerFactory.getLogger(VPController.class);

    private final VPConverter converter;
    private final VPService service;


    @PostMapping(path = "/f/{feedId}/r/{routeId}/t/{tripId}/d/{direction}")
    public void sendVP(@PathVariable String feedId, @PathVariable String routeId,
                       @PathVariable String tripId, @PathVariable String direction,
                       @RequestBody VPDto vpDto) {

        VehiclePosition vp = converter.vpDtoToEntity(vpDto, feedId, routeId, tripId, direction);
        service.sendToKafka(vp);
    }

    /**
     * Accepts GTFS-RT protobuf FeedEntity and sends vehicle position to Kafka.
     * Supports both binary protobuf and ASCII text format for debugging.
     * - Content-Type: application/x-protobuf → binary format (production)
     * - Content-Type: text/plain → ASCII text format (development/debug)
     *
     * @param feedId feed identifier
     * @param agencyId agency identifier
     * @param body request body (binary protobuf or ASCII text)
     * @param contentType content type header
     */
    @PostMapping(path = "/f/{feedId}/a/{agencyId}",
                 consumes = {"application/x-protobuf", "text/plain"},
                 produces = "application/json")
    public void sendVPProto(@PathVariable String feedId,
                            @PathVariable String agencyId,
                            @RequestBody byte[] body,
                            @RequestHeader(value = "Content-Type") String contentType) {

        GtfsRealtime.FeedEntity feedEntity;

        try {
            if (contentType.contains("text/plain")) {
                // Parse ASCII text format
                logger.debug("Parsing GTFS-RT FeedEntity from ASCII text format");
                String textContent = new String(body, StandardCharsets.UTF_8);
                GtfsRealtime.FeedEntity.Builder builder = GtfsRealtime.FeedEntity.newBuilder();
                TextFormat.merge(textContent, builder);
                feedEntity = builder.build();
            } else {
                // Parse binary protobuf format
                logger.debug("Parsing GTFS-RT FeedEntity from binary protobuf format");
                feedEntity = GtfsRealtime.FeedEntity.parseFrom(body);
            }
        } catch (Exception e) {
            logger.error("Failed to parse FeedEntity: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid FeedEntity format: " + e.getMessage());
        }

        VehiclePosition vp = converter.mapFromFeedEntity(feedEntity, feedId, agencyId);

        if (vp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "FeedEntity does not contain vehicle position data");
        }

        service.sendToKafka(vp);
        logger.info("Successfully processed vehicle position: vehicleId={}, feedId={}, agencyId={}",
                    vp.getVid(), feedId, agencyId);
    }

    public VPController(VPConverter converter, VPService service) {
        this.converter = converter;
        this.service = service;
    }
}

package com.marszrut.gtfs_rt.controller;

import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.converter.VPConverter;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.dto.VPDto;
import com.marszrut.gtfs_rt.service.VPService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/vp")
public class VPController {

    private final VPConverter converter;
    private final VPService service;


    @PostMapping(path = "/f/{feedId}/r/{routeId}/t/{tripId}/d/{direction}")
    public void sendVP(@PathVariable String feedId, @PathVariable String routeId,
                       @PathVariable String tripId, @PathVariable String direction,
                       @RequestBody VPDto vpDto) {

        VehiclePosition vp = converter.vpDtoToEntity(vpDto, feedId, routeId, tripId, direction);
        //call service to pass data to kafka? or just post to topic directly?
        service.sendToKafka(vp);
    }

    /**
     * Accepts GTFS-RT protobuf FeedEntity and sends vehicle position to Kafka.
     * Same endpoint as sendVP but accepts protobuf instead of JSON.
     * Spring routes based on Content-Type header (application/x-protobuf).
     *
     * @param feedId feed identifier
     * @param agencyId agency identifier (replaces routeId, tripId, direction from JSON endpoint)
     * @param feedEntity GTFS-RT FeedEntity containing VehiclePosition
     */
    @PostMapping(path = "/f/{feedId}/a/{agencyId}",
                 consumes = "application/x-protobuf",
                 produces = "application/json")
    public void sendVPProto(@PathVariable String feedId,
                            @PathVariable String agencyId,
                            @RequestBody GtfsRealtime.FeedEntity feedEntity) {
        VehiclePosition vp = converter.mapFromFeedEntity(feedEntity, feedId, agencyId);

        if (vp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "FeedEntity does not contain vehicle position data");
        }

        service.sendToKafka(vp);
    }

    public VPController(VPConverter converter, VPService service) {
        this.converter = converter;
        this.service = service;
    }
}

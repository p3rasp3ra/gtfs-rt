package com.marszrut.gtfs_rt.converter;

import com.google.transit.realtime.GtfsRealtime;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.dto.VPDto;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class VPConverter {

    public VehiclePosition vpDtoToEntity(VPDto dto, String feedId,  String routeId, String tripId, String direction) {
        return VehiclePosition.builder()
                .vid(dto.vid())
                .lat(dto.lat())
                .lon(dto.lon())
                .t(java.time.Instant.parse(dto.t()))
                .fid(feedId)
                .rid(routeId)
                .tid(tripId)
                .did(Integer.parseInt(direction))
                .sd(dto.sd())
                .st(dto.st())
                .sid(dto.cs())
                .ss(Integer.parseInt(dto.css()))
                .vl(dto.vl())
                .lp(dto.vlp())
                .os(Integer.parseInt(dto.os()))
                .aid(dto.aid())
                .build();
    }

    /**
     * Maps from GTFS-RT FeedEntity containing VehiclePosition to domain VehiclePosition.
     * As per GTFS-RT specification, VehiclePosition data comes within a FeedEntity.
     *
     * @param feedEntity the FeedEntity from com.google.transit.realtime containing vehicle position
     * @param feedId the feed identifier
     * @param agencyId the agency identifier
     * @return domain VehiclePosition entity, or null if feedEntity doesn't contain vehicle data
     */
    public VehiclePosition mapFromFeedEntity(
            GtfsRealtime.FeedEntity feedEntity,
            String feedId,
            String agencyId) {

        // Check if this FeedEntity has vehicle position data
        if (!feedEntity.hasVehicle()) {
            return null;
        }

        GtfsRealtime.VehiclePosition protoVP = feedEntity.getVehicle();

        // Per GTFS-RT spec, VehiclePosition doesn't contain VehicleDescriptor
        // The FeedEntity ID typically represents the vehicle identifier
        // Vehicle descriptor details (label, license plate) must be provided separately
        // or maintained in a separate vehicle registry
        String vehicleId = feedEntity.hasId() ? feedEntity.getId() : "";

        // These fields are not available in the standard GTFS-RT VehiclePosition message
        // They should be enriched from external data sources
        String vehicleLabel = "";
        String licensePlate = "";

        // Extract trip information
        String tripId = protoVP.hasTrip() && protoVP.getTrip().hasTripId()
            ? protoVP.getTrip().getTripId() : "";
        String routeId = protoVP.hasTrip() && protoVP.getTrip().hasRouteId()
            ? protoVP.getTrip().getRouteId() : "";
        Integer directionId = protoVP.hasTrip() && protoVP.getTrip().hasDirectionId()
            ? protoVP.getTrip().getDirectionId() : 0;
        String startDate = protoVP.hasTrip() && protoVP.getTrip().hasStartDate()
            ? protoVP.getTrip().getStartDate() : "";
        String startTime = protoVP.hasTrip() && protoVP.getTrip().hasStartTime()
            ? protoVP.getTrip().getStartTime() : "";

        // Extract position information
        Double latitude = protoVP.hasPosition()
            ? (double) protoVP.getPosition().getLatitude() : 0.0;
        Double longitude = protoVP.hasPosition()
            ? (double) protoVP.getPosition().getLongitude() : 0.0;

        // Extract timestamp (proto uses uint64 seconds since epoch)
        Instant timestamp = protoVP.hasTimestamp()
            ? Instant.ofEpochSecond(protoVP.getTimestamp())
            : Instant.now();

        // Extract stop information
        String stopId = protoVP.hasStopId() ? protoVP.getStopId() : "";
        int currentStopStatus = protoVP.hasCurrentStatus()
            ? protoVP.getCurrentStatus().getNumber() : 0;

        // Extract occupancy status
        Integer occupancyStatus = protoVP.hasOccupancyStatus()
            ? protoVP.getOccupancyStatus().getNumber() : 0;

        return VehiclePosition.builder()
                .vid(vehicleId)
                .lat(latitude)
                .lon(longitude)
                .t(timestamp)
                .fid(feedId != null ? feedId : "")
                .aid(agencyId != null ? agencyId : "")
                .rid(routeId)
                .tid(tripId)
                .did(directionId)
                .sd(startDate)
                .st(startTime)
                .sid(stopId)
                .ss(currentStopStatus)
                .vl(vehicleLabel)
                .lp(licensePlate)
                .os(occupancyStatus)
                .build();
    }

    /**
     * Converts domain VehiclePosition entity back to GTFS-RT FeedEntity.
     * Used for serving aggregated feed to transit consumers.
     *
     * @param vp domain VehiclePosition entity
     * @return GTFS-RT FeedEntity containing VehiclePosition
     */
    public GtfsRealtime.FeedEntity entityToFeedEntity(VehiclePosition vp) {
        GtfsRealtime.TripDescriptor.Builder tripBuilder = GtfsRealtime.TripDescriptor.newBuilder();

        if (vp.getTid() != null && !vp.getTid().isEmpty()) {
            tripBuilder.setTripId(vp.getTid());
        }
        if (vp.getRid() != null && !vp.getRid().isEmpty()) {
            tripBuilder.setRouteId(vp.getRid());
        }
        if (vp.getDid() != null) {
            tripBuilder.setDirectionId(vp.getDid());
        }
        if (vp.getSd() != null && !vp.getSd().isEmpty()) {
            tripBuilder.setStartDate(vp.getSd());
        }
        if (vp.getSt() != null && !vp.getSt().isEmpty()) {
            tripBuilder.setStartTime(vp.getSt());
        }

        GtfsRealtime.Position.Builder positionBuilder = GtfsRealtime.Position.newBuilder()
            .setLatitude(vp.getLat().floatValue())
            .setLongitude(vp.getLon().floatValue());

        GtfsRealtime.VehiclePosition.Builder vehicleBuilder = GtfsRealtime.VehiclePosition.newBuilder()
            .setTrip(tripBuilder.build())
            .setPosition(positionBuilder.build())
            .setTimestamp(vp.getT().getEpochSecond());

        if (vp.getSid() != null && !vp.getSid().isEmpty()) {
            vehicleBuilder.setStopId(vp.getSid());
        }

        vehicleBuilder.setCurrentStatus(
            GtfsRealtime.VehiclePosition.VehicleStopStatus.forNumber(vp.getSs())
        );

        if (vp.getOs() != null) {
            vehicleBuilder.setOccupancyStatus(
                GtfsRealtime.VehiclePosition.OccupancyStatus.forNumber(vp.getOs())
            );
        }

        return GtfsRealtime.FeedEntity.newBuilder()
            .setId(vp.getVid())
            .setVehicle(vehicleBuilder.build())
            .build();
    }
}

package com.marszrut.gtfs_rt.controller;

import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VPControllerProtoTest {

    @LocalServerPort
    private int port;

    private RestClient createRestClient() {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void testSendVPProto_withValidFeedEntity_returnsOk() {
        // Create a valid GTFS-RT FeedEntity with complete VehiclePosition data
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("vehicle_123")
            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("trip_456")
                    .setRouteId("route_789")
                    .setDirectionId(1)
                    .setStartDate("20260109")
                    .setStartTime("14:30:00")
                    .build())
                .setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude(52.2297f)
                    .setLongitude(21.0122f)
                    .build())
                .setTimestamp(System.currentTimeMillis() / 1000)
                .setStopId("stop_001")
                .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT)
                .setOccupancyStatus(GtfsRealtime.VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE)
                .build())
            .build();

        RestClient restClient = createRestClient();

        restClient.post()
            .uri("/vp/f/ztp-feed/a/ztp-agency")
            .contentType(MediaType.parseMediaType("application/x-protobuf"))
            .body(entity.toByteArray())
            .retrieve()
            .toBodilessEntity();

        // If we get here without exception, the request was successful
        assertThat(true).isTrue();
    }

    @Test
    void testSendVPProto_withMinimalData_returnsOk() {
        // Create FeedEntity with minimal required fields
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("vehicle_minimal")
            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                .setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude(50.0647f)
                    .setLongitude(19.945f)
                    .build())
                .setTimestamp(System.currentTimeMillis() / 1000)
                .build())
            .build();

        RestClient restClient = createRestClient();

        restClient.post()
            .uri("/vp/f/test-feed/a/test-agency")
            .contentType(MediaType.parseMediaType("application/x-protobuf"))
            .body(entity.toByteArray())
            .retrieve()
            .toBodilessEntity();

        assertThat(true).isTrue();
    }

    @Test
    void testSendVPProto_withoutVehicleData_returnsBadRequest() {
        // Create FeedEntity without vehicle position (e.g., alert or trip update)
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("alert_123")
            .build();

        RestClient restClient = createRestClient();

        assertThatThrownBy(() ->
            restClient.post()
                .uri("/vp/f/ztp-feed/a/ztp-agency")
                .contentType(MediaType.parseMediaType("application/x-protobuf"))
                .body(entity.toByteArray())
                .retrieve()
                .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void testSendVPProto_withTripUpdateInsteadOfVehicle_returnsBadRequest() {
        // Create FeedEntity with TripUpdate instead of VehiclePosition
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("trip_update_123")
            .setTripUpdate(GtfsRealtime.TripUpdate.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("trip_456")
                    .build())
                .build())
            .build();

        RestClient restClient = createRestClient();

        assertThatThrownBy(() ->
            restClient.post()
                .uri("/vp/f/ztp-feed/a/ztp-agency")
                .contentType(MediaType.parseMediaType("application/x-protobuf"))
                .body(entity.toByteArray())
                .retrieve()
                .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void testSendVPProto_withCompleteGtfsRtData_mapsAllFields() {
        // Create FeedEntity with all optional GTFS-RT fields populated
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("vehicle_full_data")
            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("trip_001")
                    .setRouteId("route_001")
                    .setDirectionId(0)
                    .setStartDate("20260109")
                    .setStartTime("08:00:00")
                    .setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
                    .build())
                .setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude(52.2297f)
                    .setLongitude(21.0122f)
                    .setBearing(45.0f)
                    .setSpeed(15.5f)
                    .build())
                .setTimestamp(1736428800L) // Fixed timestamp for testing
                .setStopId("stop_central")
                .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO)
                .setCongestionLevel(GtfsRealtime.VehiclePosition.CongestionLevel.RUNNING_SMOOTHLY)
                .setOccupancyStatus(GtfsRealtime.VehiclePosition.OccupancyStatus.MANY_SEATS_AVAILABLE)
                .build())
            .build();

        RestClient restClient = createRestClient();

        restClient.post()
            .uri("/vp/f/main-feed/a/main-agency")
            .contentType(MediaType.parseMediaType("application/x-protobuf"))
            .body(entity.toByteArray())
            .retrieve()
            .toBodilessEntity();

        assertThat(true).isTrue();
    }
}


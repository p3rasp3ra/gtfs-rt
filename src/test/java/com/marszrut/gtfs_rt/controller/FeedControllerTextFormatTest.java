package com.marszrut.gtfs_rt.controller;

import com.google.protobuf.TextFormat;
import com.google.transit.realtime.GtfsRealtime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GTFS-RT ASCII text format support (development/debug).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedControllerTextFormatTest {

    @LocalServerPort
    private int port;

    private RestClient createRestClient() {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    void testPostVPWithTextFormat_returnsOk() {
        // Create FeedEntity in text format
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("vehicle_text_test")
            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("trip_text_001")
                    .setRouteId("route_text_001")
                    .build())
                .setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude(50.0647f)
                    .setLongitude(19.945f)
                    .build())
                .setTimestamp(System.currentTimeMillis() / 1000)
                .build())
            .build();

        // Convert to ASCII text format
        String textFormat = TextFormat.printer().printToString(entity);

        RestClient restClient = createRestClient();

        // Send as text/plain
        restClient.post()
            .uri("/vp/f/test-feed/a/test-agency")
            .contentType(MediaType.TEXT_PLAIN)
            .body(textFormat.getBytes())
            .retrieve()
            .toBodilessEntity();

        // Wait for Redis caching
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify we can retrieve the feed in text format
        String feedText = restClient.get()
            .uri("/gtfs-rt/feed.pb")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .body(String.class);

        assertThat(feedText).isNotNull();
        assertThat(feedText).contains("vehicle_text_test");
        assertThat(feedText).contains("trip_text_001");
    }

    @Test
    void testGetFeedWithTextFormat_returnsReadableText() {
        RestClient restClient = createRestClient();

        // Request feed in text format
        String feedText = restClient.get()
            .uri("/gtfs-rt/feed.pb")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .body(String.class);

        assertThat(feedText).isNotNull();
        // Should contain header information
        assertThat(feedText).containsAnyOf("header", "gtfs_realtime_version", "timestamp");
    }

    @Test
    void testGetFeedWithBinaryFormat_returnsProtobuf() {
        RestClient restClient = createRestClient();

        // Request feed in binary format
        byte[] feedBinary = restClient.get()
            .uri("/gtfs-rt/feed.pb")
            .accept(MediaType.parseMediaType("application/x-protobuf"))
            .retrieve()
            .body(byte[].class);

        assertThat(feedBinary).isNotNull();
        assertThat(feedBinary.length).isGreaterThan(0);

        // Verify it's valid protobuf
        try {
            GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(feedBinary);
            assertThat(feedMessage.hasHeader()).isTrue();
        } catch (Exception e) {
            throw new AssertionError("Failed to parse binary feed", e);
        }
    }

    @Test
    void testContentNegotiation_defaultsToBinary() {
        RestClient restClient = createRestClient();

        // Request without explicit Accept header (should default to protobuf)
        byte[] feedBinary = restClient.get()
            .uri("/gtfs-rt/feed.pb")
            .retrieve()
            .body(byte[].class);

        assertThat(feedBinary).isNotNull();

        // Verify it's valid binary protobuf
        try {
            GtfsRealtime.FeedMessage feedMessage = GtfsRealtime.FeedMessage.parseFrom(feedBinary);
            assertThat(feedMessage.hasHeader()).isTrue();
        } catch (Exception e) {
            throw new AssertionError("Default format should be binary protobuf", e);
        }
    }
}


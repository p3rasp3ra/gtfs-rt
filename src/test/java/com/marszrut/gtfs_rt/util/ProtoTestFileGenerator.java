package com.marszrut.gtfs_rt.util;

import com.google.transit.realtime.GtfsRealtime;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility to generate test GTFS-RT protobuf files for testing the endpoint.
 * Run this class to create test_vehicle.pb file.
 */
public class ProtoTestFileGenerator {

    public static void main(String[] args) {
        try {
            generateTestVehiclePositionFile("test_vehicle.pb");
            System.out.println("✅ Successfully created test_vehicle.pb");
            System.out.println("You can now test the endpoint with:");
            System.out.println("  curl -X POST http://localhost:8087/vp/f/ztp-feed/a/ztp-agency \\");
            System.out.println("    -H \"Content-Type: application/x-protobuf\" \\");
            System.out.println("    --data-binary @test_vehicle.pb");
        } catch (IOException e) {
            System.err.println("❌ Failed to create protobuf file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateTestVehiclePositionFile(String filename) throws IOException {
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("vehicle_test_001")
            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("trip_12345")
                    .setRouteId("route_67")
                    .setDirectionId(1)
                    .setStartDate("20260109")
                    .setStartTime("14:30:00")
                    .setScheduleRelationship(GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED)
                    .build())
                .setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude(52.2297f)
                    .setLongitude(21.0122f)
                    .setBearing(180.0f)
                    .setSpeed(25.5f)
                    .build())
                .setTimestamp(System.currentTimeMillis() / 1000)
                .setStopId("stop_central_001")
                .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT)
                .setCongestionLevel(GtfsRealtime.VehiclePosition.CongestionLevel.RUNNING_SMOOTHLY)
                .setOccupancyStatus(GtfsRealtime.VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE)
                .build())
            .build();

        try (FileOutputStream output = new FileOutputStream(filename)) {
            entity.writeTo(output);
        }
    }
}


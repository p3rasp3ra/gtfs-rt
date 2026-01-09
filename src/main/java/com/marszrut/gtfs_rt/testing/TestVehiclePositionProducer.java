package com.marszrut.gtfs_rt.testing;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import com.marszrut.gtfs_rt.service.VPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Test producer to send a sample VehiclePosition message to Kafka.
 * Sends messages as VehiclePosition objects.
 *
 * To disable this, comment out the @Component annotation.
 */
@Component
public class TestVehiclePositionProducer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestVehiclePositionProducer.class);

    private final VPService vpService;

    public TestVehiclePositionProducer(VPService vpService) {
        this.vpService = vpService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Wait a bit for Kafka consumers to be ready
        Thread.sleep(5000);

        logger.info("===== SENDING TEST VEHICLE POSITION OBJECT TO KAFKA =====");

        // Create a complete test vehicle position object
        VehiclePosition vp = VehiclePosition.builder()
                .vid("TEST-VEHICLE-001")
                .lat(50.0647)
                .lon(19.9450)
                .t(Instant.now())
                .fid("test-feed")
                .aid("test-agency")
                .rid("test-route")
                .tid("test-trip")
                .did(1)
                .sd("20260104")
                .st("120000")
                .sid("test-stop")
                .ss(0)
                .vl("Test Bus #1")
                .lp("XYZ-12345")
                .os(1)
                .build();

        // Send to Kafka via VPService
        vpService.sendToKafka(vp);

        logger.info("===== TEST VEHICLE POSITION OBJECT SENT SUCCESSFULLY =====");
    }
}

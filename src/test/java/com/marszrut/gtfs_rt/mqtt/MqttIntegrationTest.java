package com.marszrut.gtfs_rt.mqtt;

import com.google.transit.realtime.GtfsRealtime;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for MQTT → Kafka → Redis flow.
 * Tests the complete data pipeline from MQTT ingestion to Redis caching.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MqttIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @DynamicPropertySource
    static void mqttProperties(DynamicPropertyRegistry registry) {
        registry.add("mqtt.broker.url", () -> "tcp://localhost:1883");
        registry.add("mqtt.username", () -> "");
        registry.add("mqtt.password", () -> "");
    }

    @Test
    void testMqttToRedisFlow() throws Exception {
        // Create test GTFS-RT FeedEntity
        GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
            .setId("test_vehicle_mqtt")
            .setVehicle(GtfsRealtime.VehiclePosition.newBuilder()
                .setTrip(GtfsRealtime.TripDescriptor.newBuilder()
                    .setTripId("trip_test")
                    .setRouteId("route_test")
                    .setDirectionId(1)
                    .setStartDate("20260109")
                    .setStartTime("15:00:00")
                    .build())
                .setPosition(GtfsRealtime.Position.newBuilder()
                    .setLatitude(50.0647f)
                    .setLongitude(19.9450f)
                    .build())
                .setTimestamp(System.currentTimeMillis() / 1000)
                .setStopId("stop_test")
                .setCurrentStatus(GtfsRealtime.VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO)
                .setOccupancyStatus(GtfsRealtime.VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE)
                .build())
            .build();

        // Publish to MQTT
        String mqttBroker = "tcp://localhost:1883";
        String clientId = "test-publisher-" + System.currentTimeMillis();
        String topic = "/gtfsrt/vp/test-feed/test-agency/TEST/BUS/route_test/1/Downtown/" +
                      "trip_test/stop_test/15:00:00/test_vehicle_mqtt/u/g/j/k/123/FF0000/";

        MqttClient mqttClient = new MqttClient(mqttBroker, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        mqttClient.connect(options);

        MqttMessage message = new MqttMessage(entity.toByteArray());
        message.setQos(1);
        mqttClient.publish(topic, message);
        mqttClient.disconnect();

        // Wait for message to flow through system: MQTT → Kafka → Fast Consumer → Redis
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                Object cached = redisTemplate.opsForValue().get("vp:test_vehicle_mqtt");
                assertThat(cached).isNotNull();
            });

        // Verify cached data
        Object cached = redisTemplate.opsForValue().get("vp:test_vehicle_mqtt");
        assertThat(cached).isNotNull();
    }
}


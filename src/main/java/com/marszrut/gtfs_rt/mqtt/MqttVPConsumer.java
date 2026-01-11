package com.marszrut.gtfs_rt.mqtt;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Minimal MQTT consumer - receives protobuf from MQTT and pushes to Kafka.
 * Topic structure: /gtfsrt/vp/{feedId}/{agencyId}/.../{vehicleId}/...
 * Follows Digitransit MQTT topic format.
 */
@Service
public class MqttVPConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MqttVPConsumer.class);
    private static final int MIN_TOPIC_PARTS = 14;

    private final KafkaTemplate<String, byte[]> protoKafkaTemplate;
    private final String protoTopic;

    public MqttVPConsumer(
            KafkaTemplate<String, byte[]> protoKafkaTemplate,
            @Value("${kafka.topics.vehicle-positions-proto}") String protoTopic) {
        this.protoKafkaTemplate = protoKafkaTemplate;
        this.protoTopic = protoTopic;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleVehiclePosition(Message<byte[]> message) {
        String mqttTopic = null;
        try {
            // Extract MQTT topic
            mqttTopic = (String) message.getHeaders().get("mqtt_receivedTopic");
            if (mqttTopic == null || mqttTopic.isEmpty()) {
                logger.error("MQTT topic header is missing or empty");
                return;
            }

            byte[] protoPayload = message.getPayload();
            if (protoPayload == null || protoPayload.length == 0) {
                logger.error("MQTT message payload is empty: topic={}", mqttTopic);
                return;
            }

            // Parse Digitransit topic structure
            String[] parts = mqttTopic.split("/");
            if (parts.length < MIN_TOPIC_PARTS) {
                logger.error("Invalid MQTT topic structure (expected {} parts, got {}): {}",
                           MIN_TOPIC_PARTS, parts.length, mqttTopic);
                return;
            }

            // Extract metadata from topic
            String feedId = parts[3];
            String agencyId = parts[4];
            String vehicleId = parts[13];

            // Validate extracted values
            if (feedId.isEmpty() || agencyId.isEmpty() || vehicleId.isEmpty()) {
                logger.error("Empty feedId, agencyId, or vehicleId in topic: {}", mqttTopic);
                return;
            }

            // Create Kafka record with metadata in headers
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                protoTopic,
                vehicleId,
                protoPayload
            );

            record.headers()
                .add("feedId", feedId.getBytes(StandardCharsets.UTF_8))
                .add("agencyId", agencyId.getBytes(StandardCharsets.UTF_8))
                .add("mqttTopic", mqttTopic.getBytes(StandardCharsets.UTF_8));

            protoKafkaTemplate.send(record);

            logger.debug("Pushed protobuf to Kafka: vehicleId={}, feedId={}, agencyId={}, payloadSize={}",
                        vehicleId, feedId, agencyId, protoPayload.length);

        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("Failed to parse MQTT topic structure: topic={}, error={}", mqttTopic, e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to process MQTT message: topic={}, error={}", mqttTopic, e.getMessage(), e);
        }
    }
}


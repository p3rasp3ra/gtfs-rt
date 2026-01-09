package com.marszrut.gtfs_rt.service;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class VPService {

    private static final Logger logger = LoggerFactory.getLogger(VPService.class);

    private final KafkaTemplate<String, VehiclePosition> kafkaTemplate;
    private final String topic;

    public VPService(KafkaTemplate<String, VehiclePosition> kafkaTemplate,
                     @Value("${kafka.topics.vehicle-positions}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendToKafka(VehiclePosition vp) {
        kafkaTemplate.send(topic, vp.getVid(), vp);
        logger.debug("Sent VehiclePosition to Kafka topic {} with key {}", topic, vp.getVid());
    }
}

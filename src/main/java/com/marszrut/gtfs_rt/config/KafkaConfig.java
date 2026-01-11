package com.marszrut.gtfs_rt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;

/**
 * Kafka configuration for consuming GTFS-RT messages.
 * Includes protobuf producer for MQTT integration.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.topics.vehicle-positions.slow-path-dlq}")
    private String slowPathDlqTopic;

    @Value("${kafka.topics.vehicle-positions.fast-path-dlq}")
    private String fastPathDlqTopic;

    private final ObjectMapper objectMapper;

    public KafkaConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a Kafka consumer factory that deserializes directly to VehiclePositionDomain objects.
     * Uses our custom deserializer instead of deprecated Spring classes.
     */
    @Bean
    public ConsumerFactory<String, VehiclePosition> vehiclePositionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new VehiclePositionDeserializer(objectMapper))
        );
    }

    /**
     * Creates the slow-path Kafka listener container factory for domain objects.
     * Dedicated factory for slow-path consumers with database persistence.
     * Routes failures to slow-path specific DLQ.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VehiclePosition> slowPathKafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, VehiclePosition> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(vehiclePositionConsumerFactory());

        // Enable manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Configure DLQ specific to slow-path (DB persistence failures)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                    (record, ex) -> new TopicPartition(slowPathDlqTopic, -1)),
                new FixedBackOff(5000L, 3L) // 3 retries with 5s between each
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Creates the default/fast-path Kafka listener container factory for domain objects.
     * Used for in-memory processing and fast operations.
     * Routes failures to fast-path specific DLQ.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VehiclePosition> kafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, VehiclePosition> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(vehiclePositionConsumerFactory());

        // Enable manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Configure DLQ for fast-path processing
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                    (record, ex) -> new TopicPartition(fastPathDlqTopic, -1)),
                new FixedBackOff(1000L, 2L) // 2 retries with 1s between each
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Creates a Kafka consumer factory for byte arrays.
     * Used by legacy listeners that need raw message bytes.
     */
    @Bean
    public ConsumerFactory<String, byte[]> byteArrayConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a Kafka listener container factory for byte arrays.
     * Used by legacy listeners that process raw bytes.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> byteArrayKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(byteArrayConsumerFactory());

        // Enable manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    @Bean
    public ProducerFactory<String, VehiclePosition> vehiclePositionProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Use JacksonJsonSerializer - it will use the default ObjectMapper or the one from Spring context
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JacksonJsonSerializer<VehiclePosition>()
        );
    }

    @Bean
    public KafkaTemplate<String, VehiclePosition> vehiclePositionKafkaTemplate() {
        return new KafkaTemplate<>(vehiclePositionProducerFactory());
    }

    /**
     * Creates a Kafka producer factory for sending Object messages (DLQ, etc).
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Use JacksonJsonSerializer - it will use the default ObjectMapper or the one from Spring context
        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JacksonJsonSerializer<Object>()
        );
    }

    /**
     * Creates a KafkaTemplate for sending messages.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka producer for protobuf messages (MQTT → Kafka).
     * Sends raw protobuf bytes to vp-proto topic.
     */
    @Bean
    public ProducerFactory<String, byte[]> protoProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, byte[]> protoKafkaTemplate() {
        return new KafkaTemplate<>(protoProducerFactory());
    }

    /**
     * Consumer factory for protobuf topic (reads raw bytes).
     */
    @Bean
    public ConsumerFactory<String, byte[]> protoConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("protoKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> protoKafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(protoConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Configure error handler with retry logic
        // Will retry 3 times with 5 seconds between attempts, then send to DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(slowPathDlqTopic, -1)),
            new FixedBackOff(5000L, 3L)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}

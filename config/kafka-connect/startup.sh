#!/bin/bash
set -e

echo "=========================================="
echo "Kafka Connect Startup Script"
echo "=========================================="

# Install MQTT Source Connector plugin
if [ ! -d "/usr/share/confluent-hub-components/confluentinc-kafka-connect-mqtt" ]; then
    echo "Installing MQTT Source Connector plugin..."
    confluent-hub install --no-prompt confluentinc/kafka-connect-mqtt:1.7.6 || \
    confluent-hub install --no-prompt confluentinc/kafka-connect-mqtt:1.7.0 || {
        echo "ERROR: Could not install MQTT connector plugin"
        exit 1
    }
    echo "✓ Plugin installed successfully!"
else
    echo "✓ MQTT connector plugin already installed, skipping download"
fi

# Wait for Kafka to be ready
echo "Waiting for Kafka broker to be accessible on internal port 19093..."
for i in {1..60}; do
    if nc -z kafka 19093 2>/dev/null; then
        echo ""
        echo "✓ Kafka broker port 19093 is accessible!"
        sleep 5
        break
    fi
    if [ $i -eq 60 ]; then
        echo "ERROR: Kafka broker did not become ready in time"
        exit 1
    fi
    sleep 2
done

echo ""
echo "Configuring Kafka Connect properties..."

# Create custom connect properties file with correct bootstrap servers
cat > /tmp/connect-distributed.properties << EOF
# Kafka broker connection - MUST use internal Docker network address
bootstrap.servers=kafka:19093

# REST API configuration (listeners replaces deprecated rest.port/rest.host.name)
listeners=http://0.0.0.0:8082
rest.advertised.host.name=localhost
rest.advertised.port=8082

# Connect cluster configuration
group.id=kafka-connect-group
config.storage.topic=connect-configs
offset.storage.topic=connect-offsets
status.storage.topic=connect-status

# Replication factors for standalone/single-node cluster
config.storage.replication.factor=1
offset.storage.replication.factor=1
status.storage.replication.factor=1

# Default converters for Connect framework
key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.storage.StringConverter
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false

# Plugin path - critical for MQTT connector discovery
plugin.path=/usr/share/java,/usr/share/confluent-hub-components

# Producer and Consumer overrides for better reliability
producer.retries=2147483647
producer.max.in.flight.requests.per.connection=5
producer.acks=all
producer.enable.idempotence=true

consumer.max.poll.records=500
consumer.max.poll.interval.ms=300000

# Error handling
errors.tolerance=none
errors.log.enable=true
errors.log.include.messages=true

# Logging
log4j.rootLogger=INFO, stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=[%d] %p %m (%c)%n
EOF

echo "✓ Custom configuration created"
echo ""
echo "Starting Kafka Connect worker..."
echo "Bootstrap servers: kafka:19093"
echo "REST API will be available at http://localhost:8082"
echo ""
echo "Use the register-connectors.bat script from your host to register MQTT connectors"
echo "=========================================="
echo ""

# Start Kafka Connect with custom config file
exec /usr/bin/connect-distributed /tmp/connect-distributed.properties

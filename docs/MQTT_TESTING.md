# MQTT Integration Testing Guide

## Quick Start

### 1. Start MQTT Broker
```bash
docker-compose up -d mosquitto
```

### 2. Start Application
```bash
.\gradlew.bat bootRun
```

### 3. Publish Test Messages
```bash
# Install dependencies
pip install gtfs-realtime-bindings paho-mqtt protobuf

# Run publisher
python scripts\mqtt_test_publisher.py --interval 5
```

Or use the batch script:
```bash
scripts\test-mqtt.bat
```

## Testing Flow

```
MQTT Publisher → MQTT Broker → Spring MQTT Consumer → Kafka (vp-proto)
                                                        ├→ Fast Consumer → Redis
                                                        └→ Slow Consumer → TimescaleDB
```

## Verify Data Flow

### 1. Check Kafka Topic
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9093 \
  --topic vp-proto \
  --from-beginning
```

### 2. Check Redis
```bash
docker exec -it redis redis-cli
> KEYS vp:*
> GET vp:vehicle_001
```

### 3. Check PostgreSQL
```bash
docker exec -it postgres psql -U gtfsuser -d gtfs_realtime_db
> SELECT * FROM vehicle_positions ORDER BY timestamp DESC LIMIT 10;
```

### 4. Check Aggregated Feed
```bash
# Binary format
curl http://localhost:8087/gtfs-rt/feed.pb -H "Accept: application/x-protobuf" --output feed.pb

# Text format (debug)
curl http://localhost:8087/gtfs-rt/feed.pb -H "Accept: text/plain"
```

## MQTT Test Publisher Options

```bash
# Basic usage
python scripts\mqtt_test_publisher.py

# Custom broker
python scripts\mqtt_test_publisher.py --broker mqtt.example.com --port 1883

# Publish 10 messages and stop
python scripts\mqtt_test_publisher.py --count 10

# Simulate 5 vehicles
python scripts\mqtt_test_publisher.py --vehicles 5

# Publish every 2 seconds
python scripts\mqtt_test_publisher.py --interval 2

# Custom feed/agency
python scripts\mqtt_test_publisher.py --feed-id prod-feed --agency-id prod-agency
```

## Topic Structure

The test publisher follows Digitransit MQTT topic structure:

```
/gtfsrt/vp/{feed_id}/{agency_id}/{agency_name}/{mode}/{route_id}/{direction_id}/{trip_headsign}/{trip_id}/{next_stop}/{start_time}/{vehicle_id}/{geohash_*}/{short_name}/{color}/
```

Example:
```
/gtfsrt/vp/ztp-feed/ztp-agency/ZTP/BUS/route_123/1/Downtown/trip_vehicle_001/stop_001/14:30:00/vehicle_001/u/g/j/k/123/FF0000/
```

## Monitoring

### Application Logs
```bash
# Watch MQTT consumer
tail -f logs/application.log | grep MqttVPConsumer

# Watch fast consumer
tail -f logs/application.log | grep VPFastConsumer

# Watch slow consumer
tail -f logs/application.log | grep VPSlowConsumer
```

### Consumer Lag
```bash
# Check Kafka consumer groups
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9093 \
  --describe --group vp-fast-consumer

docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9093 \
  --describe --group vp-slow-consumer
```

## Troubleshooting

### MQTT Connection Failed
- Check if Mosquitto is running: `docker ps | grep mosquitto`
- Check logs: `docker logs mosquitto`
- Verify port: `netstat -an | findstr :1883`

### No Messages in Kafka
- Check Spring Integration logs for MQTT connection
- Verify MQTT topic subscription: `/gtfsrt/vp/#`
- Check MqttVPConsumer is receiving messages

### Redis Empty
- Check VPFastConsumer logs
- Verify Redis is running: `docker exec redis redis-cli ping`
- Check TTL: `docker exec redis redis-cli TTL vp:vehicle_001`

### PostgreSQL Empty
- Check VPSlowConsumer logs
- Verify database connection in application.properties
- Check for constraint violations in logs

## Integration Test

Run the integration test:
```bash
.\gradlew.bat test --tests MqttIntegrationTest
```

This tests the complete flow: MQTT → Kafka → Redis

## Configuration

Update `application.properties`:

```properties
# MQTT Broker
mqtt.broker.url=tcp://localhost:1883
mqtt.username=
mqtt.password=
mqtt.qos=1

# Kafka Topics
kafka.topics.vehicle-positions-proto=vp-proto

# Consumer Groups
kafka.consumer.group-id-fast=vp-fast-consumer
kafka.consumer.group-id-slow=vp-slow-consumer

# Redis Cache
gtfs.feed.cache.ttl-seconds=60
```

## Performance Testing

Load test with multiple vehicles:
```bash
# Simulate 50 vehicles publishing every second
python scripts\mqtt_test_publisher.py --vehicles 50 --interval 1
```

Monitor:
- Kafka lag
- Redis memory usage
- PostgreSQL write throughput
- Application CPU/memory

## Production Checklist

- [ ] Configure production MQTT broker URL
- [ ] Set MQTT username/password
- [ ] Adjust consumer group instances based on load
- [ ] Set appropriate Redis TTL
- [ ] Configure TimescaleDB retention policies
- [ ] Set up monitoring/alerting
- [ ] Test failover scenarios
- [ ] Load test with expected message rate


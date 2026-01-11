# MQTT Integration Implementation Summary

## ✅ What Was Implemented

### Architecture
```
MQTT (Digitransit) → Kafka (vp-proto) → [Fast Consumer → Redis] + [Slow Consumer → TimescaleDB]
```

### Components Created

#### 1. **MqttVPConsumer** (`mqtt/MqttVPConsumer.java`)
- Minimal MQTT listener
- Parses Digitransit topic structure
- Pushes raw protobuf to Kafka with metadata in headers
- No business logic - pure pass-through

#### 2. **MqttConfig** (`config/MqttConfig.java`)
- MQTT broker connection setup
- Subscribes to `/gtfsrt/vp/#`
- QoS, reconnection, authentication

#### 3. **VPFastConsumer** (`consumer/VPFastConsumer.java`)
- Reads from `vp-proto` topic
- Converts protobuf → domain object
- Caches in Redis (TTL: 60s)
- **Purpose:** Fast feed aggregation

#### 4. **VPSlowConsumer** (`consumer/VPSlowConsumer.java`)
- Reads from `vp-proto` topic (separate consumer group)
- Converts protobuf → domain object
- Saves to TimescaleDB via JPA
- **Purpose:** Full history storage

#### 5. **VPRepository** (`repository/VPRepository.java`)
- JPA repository for TimescaleDB persistence

#### 6. **KafkaConfig Updates**
- Added protobuf producer (byte array)
- Added protobuf consumer factory
- Separate container factories for fast/slow consumers

### Configuration Added (`application.properties`)

```properties
# MQTT Configuration
mqtt.broker.url=tcp://localhost:1883
mqtt.client.id=gtfs-rt-service
mqtt.username=
mqtt.password=
mqtt.qos=1

# Kafka Topics
kafka.topics.vehicle-positions-proto=vp-proto

# Consumer Groups
kafka.consumer.group-id-fast=vp-fast-consumer
kafka.consumer.group-id-slow=vp-slow-consumer
```

## Data Flow

### 1. MQTT → Kafka
```
Driver sends protobuf to MQTT:
  Topic: /gtfsrt/vp/{feedId}/{agencyId}/.../{vehicleId}/...
  Payload: FeedEntity (protobuf binary)
    ↓
MqttVPConsumer:
  - Extracts metadata from topic
  - Pushes to Kafka with headers (feedId, agencyId)
    ↓
Kafka Topic: vp-proto
  Key: vehicleId
  Value: FeedEntity (protobuf)
  Headers: feedId, agencyId
```

### 2. Fast Path (Redis Caching)
```
VPFastConsumer:
  - Reads from vp-proto
  - Deserializes protobuf
  - Converts to VehiclePosition
  - Caches in Redis (vp:{vehicleId})
    ↓
Redis: Latest position per vehicle
  TTL: 60 seconds
    ↓
FeedController: Serves aggregated feed
  GET /gtfs-rt/feed.pb
```

### 3. Slow Path (TimescaleDB)
```
VPSlowConsumer:
  - Reads from vp-proto (separate group)
  - Deserializes protobuf
  - Converts to VehiclePosition
  - Saves to TimescaleDB
    ↓
PostgreSQL/TimescaleDB: Full history
```

## Key Design Decisions

✅ **Kafka Headers for Metadata** - feedId, agencyId preserved without modifying protobuf  
✅ **Single Proto Topic** - Both fast and slow consumers read from `vp-proto`  
✅ **Separate Consumer Groups** - Independent processing, no interference  
✅ **Minimal MQTT Consumer** - Just push to Kafka (fast, no bottleneck)  
✅ **Independent Consumers** - Can scale separately based on load  

## No Changes Needed

✅ **VPConverter** - Already has `mapFromFeedEntity()`  
✅ **FeedController** - Already serves aggregated feed  
✅ **FeedService** - Already reads from Redis  
✅ **Domain Model** - No changes  
✅ **Database Schema** - No changes  

## Configuration Required

Update these in `application.properties`:

```properties
# Set your actual MQTT broker
mqtt.broker.url=tcp://your-mqtt-broker:1883
mqtt.username=your-user
mqtt.password=your-pass
```

## Testing

### 1. Local Testing
- Start MQTT broker, Kafka, Redis, PostgreSQL
- Publish protobuf to MQTT topic
- Verify message in Kafka: `kafka-console-consumer --topic vp-proto`
- Check Redis: `redis-cli KEYS vp:*`
- Check PostgreSQL: `SELECT * FROM vehicle_positions`

### 2. Monitor Logs
- MqttVPConsumer: MQTT → Kafka push
- VPFastConsumer: Redis caching
- VPSlowConsumer: DB persistence

## Performance

| Component | Throughput | Scalability |
|-----------|-----------|-------------|
| MQTT Consumer | High | N/A (single instance) |
| Kafka Topic | Very High | Partitioned |
| Fast Consumer | High | Scale horizontally |
| Slow Consumer | Medium | Scale horizontally |
| Redis | Very High | In-memory |
| TimescaleDB | Medium | Hypertable optimized |

## Next Steps

1. Configure MQTT broker URL and credentials
2. Start all services
3. Test with sample MQTT messages
4. Monitor consumer lag
5. Scale consumers if needed

---

**Status:** ✅ Implementation complete, ready for testing


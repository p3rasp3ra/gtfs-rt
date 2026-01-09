# GTFS-RT Infrastructure Setup - COMPLETE ✅

## Current Status: Infrastructure Ready

### ✅ All Services Running Successfully

#### Core Infrastructure
- **Kafka Broker** - Port 9093 (Healthy, KRaft mode)
- **Kafka Connect** - Port 8082 (Healthy)
  - MQTT Plugin: Installed ✅
  - Registered Connectors: 3/3 ✅
    - `mqtt-vehicle-position-source`
    - `mqtt-trip-update-source`
    - `mqtt-service-alert-source`
- **MQTT Broker (Mosquitto)** - Port 1883 (Running)
- **Redis** - Port 6379 (Running)
- **TimescaleDB** - Port 5432 (Running)

#### Monitoring & Management
- **Kafka UI** - http://localhost:8080 (Running)
- **Kafka Exporter** - Port 9308
- **MQTT Exporter** - Port 9234
- **Redis Exporter** - Port 9121
- **PostgreSQL Exporter** - Port 9187

### 📋 Next Steps

#### 1. Test MQTT → Kafka Flow (Now!)

The GTFS-RT topics (`gtfsrt.vp.raw`, `gtfsrt.tu.raw`, `gtfsrt.sa.raw`) will be **automatically created** when the first message is published to MQTT.

**Test with MQTTX or mosquitto_pub:**

```bash
# Publish a test Vehicle Position message (JSON format)
mosquitto_pub -h localhost -p 1883 -t "ingest/vp/driver123" -m '{
  "vehicle": {
    "id": "vehicle-001",
    "label": "Bus 42"
  },
  "position": {
    "latitude": 59.3293,
    "longitude": 18.0686,
    "bearing": 90.0,
    "speed": 12.5
  },
  "timestamp": 1729094400
}'
```

**Verify the message reached Kafka:**
```cmd
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9093 --topic gtfsrt.vp.raw --from-beginning
```

**Or check in Kafka UI:**
- Open http://localhost:8080
- Navigate to Topics → gtfsrt.vp.raw
- Click "Messages" tab

#### 2. Implement Spring Boot Application

Following the architecture in `docs/agents.md`, implement the processing pipeline:

**Priority Order:**
1. **VehiclePositionDomain** entity (domain model)
2. **VpKafkaListener** (Kafka consumer - entry point)
3. **EnrichmentService** (deserialize JSON → domain object)
4. **StateUpdateService** (write to Redis cache)
5. **HistoryWriter** (write to TimescaleDB)

**Package Structure:**
```
src/main/java/com/marszrut/gtfs_rt/
├── domain/
│   └── VehiclePositionDomain.java
├── ingestion/
│   └── VpKafkaListener.java
├── processing/
│   ├── EnrichmentService.java
│   ├── StateUpdateService.java
│   └── HistoryWriter.java
└── data/
    └── repository/
        └── VpTimescaleRepository.java
```

#### 3. Database Schema Setup

Create TimescaleDB tables for storing historical GTFS-RT data:

```sql
-- Vehicle Positions History (Hypertable for time-series data)
CREATE TABLE vehicle_positions (
    id BIGSERIAL,
    vehicle_id VARCHAR(50) NOT NULL,
    agency_id VARCHAR(50) NOT NULL,
    route_id VARCHAR(50),
    trip_id VARCHAR(100),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    bearing FLOAT,
    speed FLOAT,
    timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Convert to TimescaleDB hypertable
SELECT create_hypertable('vehicle_positions', 'timestamp');

-- Create indexes for efficient queries
CREATE INDEX idx_vp_vehicle_id ON vehicle_positions (vehicle_id, timestamp DESC);
CREATE INDEX idx_vp_trip_id ON vehicle_positions (trip_id, timestamp DESC);
```

#### 4. Redis Key Structure

**Current State Storage:**
```
vp:{agencyId}:{vehicleId} → VehiclePositionDomain (JSON or Hash)
tu:{agencyId}:{tripId} → TripUpdateDomain
sa:{agencyId}:{alertId} → ServiceAlertDomain
```

**TTL:** Set appropriate TTL (e.g., 30 minutes) to automatically clean up stale data.

### 🔧 Configuration Summary

#### Kafka Topics (Auto-created by connectors)
- `gtfsrt.vp.raw` - Vehicle Position messages from MQTT
- `gtfsrt.tu.raw` - Trip Update messages from MQTT
- `gtfsrt.sa.raw` - Service Alert messages from MQTT

#### MQTT Topic Routing
- `ingest/vp/#` → Kafka: `gtfsrt.vp.raw`
- `ingest/tu/#` → Kafka: `gtfsrt.tu.raw`
- `ingest/sa/#` → Kafka: `gtfsrt.sa.raw`

#### Kafka Configuration
- **Port:** 9093 (changed from default 9092 due to port conflict)
- **Advertised Listener:** `kafka:9093` (Docker network)
- **Mode:** KRaft (no Zookeeper required)

#### Spring Boot Configuration
```properties
spring.kafka.bootstrap-servers=localhost:9093
spring.kafka.consumer.group-id=gtfs-rt-processor
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
```

### 🐛 Troubleshooting Commands

```cmd
# Check all container statuses
docker ps --format "table {{.Names}}\t{{.Status}}"

# View Kafka Connect logs
docker logs kafka-connect --tail 50

# List Kafka topics
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9093 --list

# Check connector status
curl http://localhost:8082/connectors/mqtt-vehicle-position-source/status

# Consume messages from a topic
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9093 --topic gtfsrt.vp.raw --from-beginning

# Test MQTT connection
docker exec mqtt-broker mosquitto_pub -t "test" -m "hello"

# Check Redis keys
docker exec redis redis-cli KEYS "*"

# Access TimescaleDB
docker exec -it timescaledb psql -U gtfsuser -d gtfs_realtime_db
```

### 📊 Architecture Flow

```
Driver App (MQTTX)
  ↓ Publishes JSON to: ingest/vp/driver123
Mosquitto MQTT Broker (port 1883)
  ↓ Subscribed by: Kafka Connect MQTT Source Connector
Kafka Topic: gtfsrt.vp.raw (port 9093)
  ↓ Consumed by: Spring Boot VpKafkaListener
EnrichmentService
  ├─→ StateUpdateService → Redis (Current State)
  └─→ HistoryWriter → TimescaleDB (Historical Record)
```

### 🎯 Infrastructure Setup: COMPLETE ✅

**All infrastructure components are operational and ready for application development!**

Next: Implement the Spring Boot Kafka consumers following the contracts in `docs/agents.md`.

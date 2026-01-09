# GTFS-RT Stack Setup - Summary

## ✅ Completed Setup

### Infrastructure Services (All Running)
- **Kafka** - Running on port 9093 (KRaft mode, single node)
- **MQTT Broker** (Mosquitto) - Running on port 1883
- **Redis** - Running on port 6379
- **TimescaleDB** - Running on port 5432
- **Kafka UI** - Running on port 8080

### Monitoring/Exporters (All Running)
- **Kafka Exporter** - Port 9308
- **MQTT Exporter** - Port 9234
- **Redis Exporter** - Port 9121
- **PostgreSQL Exporter** - Port 9187

### Kafka Connect
- **Status**: Currently starting up (takes 30-60 seconds)
- **MQTT Plugin**: ✅ Successfully installed (version 1.7.6)
- **Expected Connectors**: 3 MQTT source connectors will be auto-registered
  - mqtt-vehicle-position-source (ingest/vp/# → gtfsrt.vp.raw)
  - mqtt-trip-update-source (ingest/tu/# → gtfsrt.tu.raw)
  - mqtt-service-alert-source (ingest/sa/# → gtfsrt.sa.raw)

## 🔧 Configuration Changes Made

### 1. Port Changes
- **Kafka**: Changed from 9092 to 9093 (due to port conflict with existing process)
- **Spring Boot application.properties**: Updated to use kafka port 9093

### 2. Kafka Connect Improvements
- Added `CONNECT_REST_ADVERTISED_HOST_NAME` environment variable
- Implemented robust startup script with:
  - Network connectivity check (netcat)
  - Automatic MQTT plugin installation
  - Automatic connector registration
  - Detailed logging and error diagnostics

### 3. Health Checks Optimized
- Kafka health check: 15s start period, 3s intervals, 10 retries
- Kafka Connect health check: 60s start period, 10s intervals
- Kafka Connect waits for Kafka to be healthy before starting

## 📋 Next Steps (Once Kafka Connect is Ready)

### 1. Verify Stack Status
Run: `check-full-stack.cmd`

Expected output:
- All services: HEALTHY
- Kafka Connect: Shows 3 registered connectors
- Kafka Topics: gtfsrt.vp.raw, gtfsrt.tu.raw, gtfsrt.sa.raw

### 2. Test MQTT → Kafka Flow

**Using MQTTX or mosquitto_pub:**
```bash
# Publish a test Vehicle Position message
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
  "timestamp": 1704067200
}'
```

**Verify in Kafka UI (http://localhost:8080):**
1. Navigate to Topics → gtfsrt.vp.raw
2. Check Messages tab - should see the JSON message

**Or verify via command line:**
```cmd
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9093 --topic gtfsrt.vp.raw --from-beginning
```

### 3. Implement Spring Boot Kafka Consumer (VpKafkaListener)

Following the architecture in `docs/agents.md`:

**Package structure:**
```
src/main/java/com/marszrut/gtfs_rt/
├── ingestion/
│   └── VpKafkaListener.java (Entry point)
├── processing/
│   ├── EnrichmentService.java (Deserialize + enrich)
│   ├── StateUpdateService.java (Write to Redis)
│   └── HistoryWriter.java (Write to TimescaleDB)
└── domain/
    └── VehiclePositionDomain.java (Domain model)
```

**Implementation order:**
1. Create VehiclePositionDomain entity
2. Implement VpKafkaListener (consume from gtfsrt.vp.raw)
3. Implement EnrichmentService (deserialize JSON)
4. Implement StateUpdateService (Redis write)
5. Implement HistoryWriter (TimescaleDB write)

## 🛠️ Troubleshooting

### Check Kafka Connect Status
```cmd
docker logs kafka-connect --tail 100
curl http://localhost:8082/connectors
```

### Check Kafka Topics
```cmd
docker exec kafka kafka-topics --bootstrap-server localhost:9093 --list
```

### Check Connector Status
```cmd
curl http://localhost:8082/connectors/mqtt-vehicle-position-source/status
```

### Restart Individual Service
```cmd
docker compose restart kafka-connect
docker compose restart kafka
```

### Full Stack Restart
```cmd
docker compose down
docker compose up -d
```

## 📊 Access Points

- **Kafka UI**: http://localhost:8080
- **Kafka Connect REST API**: http://localhost:8082
- **MQTT Broker**: localhost:1883
- **Redis**: localhost:6379
- **TimescaleDB**: localhost:5432 (user: gtfsuser, db: gtfs_realtime_db)

## 🎯 Current Status

**Phase**: Infrastructure Setup ✅ COMPLETE

**Next Phase**: Spring Boot Application Development
- Implement Kafka consumers for VP, TU, SA
- Implement enrichment and validation logic
- Implement Redis caching layer
- Implement TimescaleDB persistence
- Add monitoring and metrics

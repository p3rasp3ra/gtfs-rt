# GTFS-RT Realtime Processing System

Real-time vehicle position processing system with GTFS-RT protobuf support, MQTT ingestion, and feed aggregation for transit data consumers.

## 🚀 Quick Start

1. **Start Infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Start Application**
   ```bash
   .\gradlew.bat bootRun
   ```

3. **Test MQTT Integration**
   ```bash
   scripts\test-mqtt.bat
   ```

📖 **[Complete Getting Started Guide](GETTING_STARTED.md)**

## 📊 Architecture

```
MQTT Broker (Digitransit format)
    ↓
Spring MQTT Consumer
    ↓
Kafka Topic (vp-proto)
    ├─→ Fast Consumer → Redis → GET /gtfs-rt/feed.pb
    └─→ Slow Consumer → TimescaleDB
```

## 🎯 Features

- ✅ **MQTT Integration** - Digitransit topic structure support
- ✅ **Dual Processing Paths** - Fast (Redis) + Slow (TimescaleDB)
- ✅ **GTFS-RT Protobuf** - Binary and text format support
- ✅ **Feed Aggregation** - Serves aggregated feed to transit consumers
- ✅ **HTTP Caching** - 304 Not Modified support
- ✅ **REST API** - Protobuf and JSON endpoints

## 📚 Documentation

### Getting Started
- **[Quick Start Guide](GETTING_STARTED.md)** - Get up and running in 3 steps
- **[MQTT Testing Guide](MQTT_TESTING.md)** - How to test MQTT integration

### Architecture & Implementation
- **[MQTT Implementation](MQTT_IMPLEMENTATION.md)** - MQTT consumer architecture
- **[Protobuf API](PROTOBUF_IMPLEMENTATION.md)** - REST API with protobuf support
- **[Feed Aggregation](FEED_AGGREGATION.md)** - Serving aggregated feeds

### Existing Documentation
- **[Deployment Configuration](DEPLOYMENT_CONFIG.md)**
- **[Deployment Modes](DEPLOYMENT_MODES.md)**
- **[DLQ Implementation](DLQ_IMPLEMENTATION.md)**
- **[Flyway Migration](FLYWAY_MIGRATION_STATUS.md)**
- **[Implementation Status](IMPLEMENTATION_STATUS.md)**
- **[Infrastructure](INFRASTRUCTURE_COMPLETE.md)**
- **[Setup Summary](SETUP_SUMMARY.md)**

## 🛠️ Testing Scripts

All test scripts are located in the `scripts/` folder:

### MQTT Testing
- `scripts/mqtt_test_publisher.py` - Publish test protobuf messages to MQTT
- `scripts/test-mqtt.bat` - Quick MQTT test (Windows)

### REST API Testing
- `scripts/test_proto_vp.py` - Test protobuf REST endpoint
- `scripts/test-proto-vp.bat` - Protobuf API test
- `scripts/test-text-format.bat` - Text format API test
- `scripts/test-rest-api.bat` - General REST API test
- `scripts/test-simple.bat` - Simple integration test

### Test Data
- `scripts/test_vehicle.txt` - Sample vehicle position (text format)

## 🔧 Configuration

Key configuration in `src/main/resources/application.properties`:

```properties
# MQTT Broker
mqtt.broker.url=tcp://localhost:1883

# Kafka Topics
kafka.topics.vehicle-positions-proto=vp-proto
kafka.topics.vehicle-positions=vehicle-positions

# Consumer Groups
kafka.consumer.group-id-fast=vp-fast-consumer
kafka.consumer.group-id-slow=vp-slow-consumer

# Redis Cache
gtfs.feed.cache.ttl-seconds=60
```

## 🌐 API Endpoints

### POST - Ingest Vehicle Position
```bash
# Protobuf (production)
POST /vp/f/{feedId}/a/{agencyId}
Content-Type: application/x-protobuf

# Text format (debug)
POST /vp/f/{feedId}/a/{agencyId}
Content-Type: text/plain
```

### GET - Aggregated Feed
```bash
# Binary protobuf (production)
GET /gtfs-rt/feed.pb
Accept: application/x-protobuf

# Text format (debug)
GET /gtfs-rt/feed.pb
Accept: text/plain
```

## 🗂️ Project Structure

```
gtfs-rt/
├── docs/                   # All documentation
├── scripts/                # Test scripts and tools
├── config/                 # Configuration files (Kafka, MQTT, etc.)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/marszrut/gtfs_rt/
│   │   │       ├── config/      # Spring configuration
│   │   │       ├── consumer/    # Kafka consumers
│   │   │       ├── controller/  # REST controllers
│   │   │       ├── converter/   # Data converters
│   │   │       ├── domain/      # Entity models
│   │   │       ├── mqtt/        # MQTT consumers
│   │   │       ├── repository/  # Data repositories
│   │   │       └── service/     # Business logic
│   │   └── resources/
│   │       └── db/migration/    # Flyway migrations
│   └── test/               # Unit and integration tests
├── compose.yaml            # Docker Compose setup
└── build.gradle            # Gradle build configuration
```

## 🧪 Testing

### Run Unit Tests
```bash
.\gradlew.bat test
```

### Run Integration Tests
```bash
.\gradlew.bat test --tests MqttIntegrationTest
.\gradlew.bat test --tests VPControllerProtoTest
```

### Manual Testing
```bash
# Test MQTT flow
scripts\test-mqtt.bat

# Test REST API
scripts\test-proto-vp.bat

# Test text format
scripts\test-text-format.bat
```

## 📦 Tech Stack

- **Java 24** - Latest LTS
- **Spring Boot 4.0** - Framework
- **Kafka** - Message streaming
- **MQTT (Mosquitto)** - IoT messaging
- **Redis** - Caching layer
- **PostgreSQL + TimescaleDB** - Time-series database
- **Protobuf** - GTFS-RT format
- **Docker Compose** - Infrastructure

## 🚦 Status

- ✅ MQTT Integration - Complete
- ✅ Protobuf Support - Complete
- ✅ Feed Aggregation - Complete
- ✅ Fast/Slow Consumers - Complete
- ✅ REST API - Complete
- ✅ Testing Tools - Complete
- ✅ Documentation - Complete

## 📝 License

This project is part of the marszrut.com realtime zoo 🦁

---

**Need help?** Check the [Getting Started Guide](GETTING_STARTED.md) or [MQTT Testing Guide](MQTT_TESTING.md)


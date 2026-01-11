# 🚀 MQTT Integration - Quick Start Guide

## ✅ What's Ready

### Components Implemented
- ✅ **MqttVPConsumer** - MQTT → Kafka (minimal, fast)
- ✅ **VPFastConsumer** - Kafka → Redis (latest positions)
- ✅ **VPSlowConsumer** - Kafka → TimescaleDB (full history)
- ✅ **MqttConfig** - MQTT broker connection
- ✅ **KafkaConfig** - Protobuf producer/consumer
- ✅ **VPRepository** - Database persistence

### Infrastructure Ready
- ✅ **MQTT Broker** (Mosquitto) - Already in compose.yaml
- ✅ **Configuration** - application.properties updated
- ✅ **Topic Structure** - Digitransit format supported

### Testing Tools
- ✅ **scripts/mqtt_test_publisher.py** - Python MQTT publisher
- ✅ **scripts/test-mqtt.bat** - Quick start script
- ✅ **MqttIntegrationTest** - Automated test
- ✅ **docs/MQTT_TESTING.md** - Complete guide

## 🎯 Quick Start (3 Steps)

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Start Application
```bash
.\gradlew.bat bootRun
```

### 3. Test MQTT Flow
```bash
# Option A: Use batch script
scripts\test-mqtt.bat

# Option B: Manual
pip install gtfs-realtime-bindings paho-mqtt protobuf
python scripts\mqtt_test_publisher.py --interval 5
```

## 📊 Verify Everything Works

### Check Logs
```bash
# Spring app should show:
# - "Connected to MQTT broker"
# - "Pushed protobuf to Kafka"
# - "Cached VP in Redis"
# - "Saved VP to TimescaleDB"
```

### Check Data

**Redis (Fast Path):**
```bash
docker exec redis redis-cli
> KEYS vp:*
> GET vp:vehicle_001
```

**PostgreSQL (Slow Path):**
```bash
docker exec postgres psql -U gtfsuser -d gtfs_realtime_db
> SELECT vehicle_id, latitude, longitude, timestamp FROM vehicle_positions ORDER BY timestamp DESC LIMIT 5;
```

**Aggregated Feed:**
```bash
# Text format (debug)
curl http://localhost:8087/gtfs-rt/feed.pb -H "Accept: text/plain"

# Binary format (production)
curl http://localhost:8087/gtfs-rt/feed.pb -H "Accept: application/x-protobuf" --output feed.pb
```

## 🔧 Configuration

Update these in `application.properties` for production:

```properties
# MQTT Broker (currently localhost)
mqtt.broker.url=tcp://your-production-broker:1883
mqtt.username=your-username
mqtt.password=your-password
```

## 📈 Data Flow

```
Driver App
    ↓ (publishes protobuf)
MQTT Broker (port 1883)
    ↓ (subscribes to /gtfsrt/vp/#)
MqttVPConsumer (minimal, just forwards)
    ↓ (metadata in headers: feedId, agencyId)
Kafka Topic: vp-proto
    ├─→ VPFastConsumer (group: vp-fast-consumer)
    │       ↓
    │   Redis (latest per vehicle, TTL: 60s)
    │       ↓
    │   GET /gtfs-rt/feed.pb (aggregated feed)
    │
    └─→ VPSlowConsumer (group: vp-slow-consumer)
            ↓
        TimescaleDB (full history)
```

## 🎓 Key Design Points

1. **MQTT Consumer** - Minimal, no business logic, just push to Kafka
2. **Metadata in Headers** - feedId, agencyId preserved without modifying protobuf
3. **Separate Consumer Groups** - Fast and slow paths independent
4. **Redis Caching** - Fast feed aggregation (60s TTL)
5. **TimescaleDB** - Full historical data for analysis

## 📝 Project Structure

```
gtfs-rt/
├── docs/                           # Documentation
│   ├── GETTING_STARTED.md          # This file
│   ├── MQTT_IMPLEMENTATION.md      # Architecture details
│   ├── MQTT_TESTING.md             # Testing guide
│   ├── PROTOBUF_IMPLEMENTATION.md  # Protobuf API docs
│   └── FEED_AGGREGATION.md         # Feed aggregation docs
├── scripts/                        # Testing scripts
│   ├── mqtt_test_publisher.py      # MQTT test publisher
│   ├── test_proto_vp.py            # Protobuf test script
│   ├── test-mqtt.bat               # MQTT quick test
│   ├── test-proto-vp.bat           # Protobuf test
│   ├── test-text-format.bat        # Text format test
│   ├── test-rest-api.bat           # REST API test
│   ├── test-simple.bat             # Simple test
│   └── test_vehicle.txt            # Sample text format data
└── src/                            # Source code
```

## 🐛 Troubleshooting

### Issue: MQTT connection failed
**Solution:** Check if Mosquitto is running
```bash
docker ps | grep mosquitto
docker logs mosquitto
```

### Issue: No messages in Kafka
**Solution:** Check MqttVPConsumer logs
```bash
# Look for "Pushed protobuf to Kafka"
```

### Issue: Redis empty
**Solution:** Check VPFastConsumer logs
```bash
# Look for "Cached VP in Redis"
```

### Issue: PostgreSQL empty
**Solution:** Check VPSlowConsumer logs
```bash
# Look for "Saved VP to TimescaleDB"
```

## 🚀 Ready for Production?

### Checklist
- [ ] Update MQTT broker URL to production
- [ ] Configure MQTT authentication (username/password)
- [ ] Test with real vehicle data
- [ ] Monitor Kafka consumer lag
- [ ] Verify Redis TTL is appropriate
- [ ] Check TimescaleDB storage/retention
- [ ] Set up monitoring alerts
- [ ] Load test with expected message volume

---

**Status: ✅ READY TO TEST**

Start with: `scripts\test-mqtt.bat` and watch the magic happen! 🎉

## 📚 Further Reading

- [MQTT Implementation Details](./MQTT_IMPLEMENTATION.md)
- [Testing Guide](./MQTT_TESTING.md)
- [Protobuf API](./PROTOBUF_IMPLEMENTATION.md)
- [Feed Aggregation](./FEED_AGGREGATION.md)


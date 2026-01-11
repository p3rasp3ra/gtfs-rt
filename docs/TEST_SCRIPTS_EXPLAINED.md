# Test Scripts Explained

## 🔀 Two Different Test Scenarios

### 1. **mqtt_test_publisher.py** - MQTT Testing ✅

**Purpose:** Test MQTT integration (driver apps → MQTT broker)

**What it does:**
- Publishes protobuf to **MQTT broker** (port 1883)
- Uses **Digitransit topic structure**:
  ```
  /gtfsrt/vp/{feed_id}/{agency_id}/{agency_name}/{mode}/{route_id}/{direction_id}/
  {trip_headsign}/{trip_id}/{next_stop}/{start_time}/{vehicle_id}/
  {geohash_*}/{short_name}/{color}/
  ```
- Example topic:
  ```
  /gtfsrt/vp/ztp-feed/ztp-agency/ZTP/BUS/route_123/1/Downtown/trip_vehicle_001/stop_001/14:30:00/vehicle_001/u/g/j/k/123/FF0000/
  ```

**Use this to test:**
- MQTT → Kafka flow
- MqttVPConsumer
- Full data pipeline

**Run:**
```bash
python scripts\mqtt_test_publisher.py --broker localhost --interval 5
```

---

### 2. **test_proto_vp.py** - REST API Testing ✅

**Purpose:** Test HTTP REST API (external systems → HTTP endpoint)

**What it does:**
- Sends HTTP POST to **REST API** (port 8087)
- Uses **REST endpoint**:
  ```
  POST /vp/f/{feedId}/a/{agencyId}
  Content-Type: application/x-protobuf
  ```
- Example URL:
  ```
  http://localhost:8087/vp/f/ztp-feed/a/ztp-agency
  ```

**Use this to test:**
- REST API protobuf support
- VPController endpoint
- Direct HTTP integration

**Run:**
```bash
python scripts\test_proto_vp.py
```

---

## 🎯 Which One Should You Use?

### For MQTT Integration Testing
✅ **Use `mqtt_test_publisher.py`**
- Tests the real data flow: MQTT → Kafka → Redis/DB
- Uses Digitransit topic structure
- Simulates driver apps

### For REST API Testing
✅ **Use `test_proto_vp.py`**
- Tests HTTP endpoints
- No MQTT broker needed
- Simulates direct API calls

---

## 📊 Data Flow Comparison

### MQTT Flow (mqtt_test_publisher.py)
```
mqtt_test_publisher.py
    ↓ (publishes protobuf)
MQTT Broker (topic: /gtfsrt/vp/...)
    ↓ (MqttVPConsumer subscribes)
Kafka Topic: vp-proto
    ↓
Fast/Slow Consumers → Redis/DB
```

### REST API Flow (test_proto_vp.py)
```
test_proto_vp.py
    ↓ (HTTP POST)
REST API: /vp/f/{feedId}/a/{agencyId}
    ↓ (VPController)
Kafka Topic: vp-proto
    ↓
Fast/Slow Consumers → Redis/DB
```

---

## 🔍 Quick Reference

| Feature | mqtt_test_publisher.py | test_proto_vp.py |
|---------|----------------------|------------------|
| **Protocol** | MQTT | HTTP/REST |
| **Port** | 1883 (MQTT) | 8087 (HTTP) |
| **Topic/URL** | Digitransit format | REST endpoint |
| **Tests** | MQTT integration | REST API |
| **Simulates** | Driver apps | External systems |

---

## ✅ Summary

**Both scripts are correct!** They test different parts of the system:

- **MQTT script** → Tests MQTT broker integration (Digitransit topics) ✅
- **REST script** → Tests HTTP API endpoints ✅

Your system supports **both** ingestion methods:
1. **MQTT** (for driver apps using Digitransit format)
2. **REST API** (for external systems using HTTP)

Both eventually flow to the same Kafka topic and processing pipeline! 🎉


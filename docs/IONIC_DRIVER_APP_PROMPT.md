# Prompt for Ionic Driver App Agent

## Context

You are implementing the **Ionic Driver App** that sends vehicle position data to a GTFS-RT backend system. The backend is a Spring Boot application that supports **three ingestion pathways**. Your app must implement all three for testing flexibility.

---

## Backend System Overview

The backend (`gtfs-rt` project) receives vehicle positions through:

1. **REST + Protobuf** → Direct HTTP POST with binary protobuf
2. **REST + JSON** → Direct HTTP POST with JSON payload
3. **MQTT + Protobuf** → Publish to MQTT broker with binary protobuf

---

## Contract Definitions

### **1. REST + Protobuf Endpoint**

```
POST /vp/f/{feedId}/a/{agencyId}
Content-Type: application/x-protobuf

Body: <binary protobuf FeedEntity>
```

**Protobuf Schema** (GTFS-RT `gtfs-realtime.proto`):

```protobuf
message FeedEntity {
  required string id = 1;
  optional VehiclePosition vehicle = 3;
}

message VehiclePosition {
  optional TripDescriptor trip = 1;
  optional VehicleDescriptor vehicle = 8;
  optional Position position = 2;
  optional uint64 timestamp = 5;
}

message Position {
  required float latitude = 1;
  required float longitude = 2;
  optional float bearing = 3;
  optional float speed = 4;
}

message VehicleDescriptor {
  optional string id = 1;
  optional string label = 2;
  optional string license_plate = 3;
}

message TripDescriptor {
  optional string trip_id = 1;
  optional string route_id = 5;
  optional uint32 direction_id = 6;
  optional string start_time = 2;
  optional string start_date = 3;
}
```

---

### **2. REST + JSON Endpoint**

```
POST /vp/f/{feedId}/r/{routeId}/t/{tripId}/d/{direction}
Content-Type: application/json

Body:
{
  "vid": "123",
  "lat": 52.2297,
  "lon": 21.0122,
  "t": "2026-01-09T14:30:00Z",
  "vl": "Bus 505",
  "lp": "WA12345",
  "sd": "20260109",
  "st": "14:30:00",
  "sid": "stop_001",
  "ss": 1,
  "os": 2
}
```

---

### **3. MQTT + Protobuf**

**Topic Structure (Digitransit format):**
```
/<feed_format>/<type>/<feed_id>/<agency_id>/<agency_name>/<mode>/<route_id>/<direction_id>/<trip_headsign>/<trip_id>/<next_stop>/<start_time>/<vehicle_id>/<geohash_head>/<geohash_firstdeg>/<geohash_seconddeg>/<geohash_thirddeg>/<short_name>/<color>/
```

**Example:**
```
/gtfsrt/vp/waw/ztm/ZTM/BUS/505/1/Downtown/trip_001/stop_001/14:30:00/vehicle_123/u/g/j/k/505/FF0000/
```

**Payload:** Binary protobuf `FeedEntity` (same as REST+Protobuf)

**QoS:** 1 (at least once)

---

## Implementation Requirements

### **1. Project Setup**

```bash
ionic start driver-app blank --type=angular --capacitor
cd driver-app
npm install @capawesome/capacitor-background-geolocation
npm install protobufjs mqtt
npm install @capacitor/preferences
```

### **2. Services to Implement**

#### **a) Location Service**
- Use Capacitor Geolocation for foreground
- Use `@capawesome/capacitor-background-geolocation` for background tracking
- Collect: latitude, longitude, bearing (heading), speed
- Update interval: configurable (default 5 seconds)

#### **b) Protocol Service**
- Build protobuf `FeedEntity` from location data
- Build JSON payload from location data
- Handle serialization/deserialization

#### **c) Transport Service**
- REST client for HTTP endpoints
- MQTT client for broker connection
- Automatic retry on failure
- Offline queue for connectivity gaps

#### **d) Configuration Service**
- Store driver/vehicle config
- Support switching between REST/MQTT modes
- Store backend URLs and MQTT broker address

### **3. Data Flow**

```
GPS Location Update
       ↓
┌──────────────────┐
│ Location Service │
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Protocol Service │ ← Build protobuf OR JSON
└────────┬─────────┘
         ↓
┌──────────────────┐
│ Transport Service│ ← REST or MQTT
└────────┬─────────┘
         ↓
    Backend API
```

### **4. Configuration Screen**

Allow driver to configure:
- `feedId` (city code, e.g., "waw")
- `agencyId` (agency code, e.g., "ztm")
- `vehicleId` (bus number)
- `routeId` (route being driven)
- `tripId` (specific trip)
- `directionId` (0 or 1)
- `licensePlate`
- Transport mode: REST_PROTO | REST_JSON | MQTT
- Backend URL / MQTT broker address
- Update interval (seconds)

### **5. Error Handling**

- Network failures → Queue locally, retry with exponential backoff
- GPS failures → Use last known position with stale flag
- MQTT disconnection → Auto-reconnect with session persistence
- Invalid config → Block sending until fixed

### **6. Testing Mode**

Implement a testing mode that:
- Sends to all three endpoints simultaneously
- Logs response times for each
- Shows success/failure status per pathway
- Allows manual position override (for desk testing)

### **7. Transport Mode UI Switch**

Implement a UI toggle/selector allowing tester to switch between:

```
┌─────────────────────────────────────┐
│  Transport Mode                     │
├─────────────────────────────────────┤
│  ○ REST + Protobuf                  │
│  ○ REST + JSON                      │
│  ○ MQTT + Protobuf                  │
│  ○ All (Testing Mode)               │
└─────────────────────────────────────┘
```

**Requirements:**
- Radio button or segmented control for mode selection
- Persist selection in local storage
- Show current mode indicator in header/status bar
- Visual feedback when mode changes (toast notification)
- "All" mode sends to all three endpoints simultaneously for comparison testing

---

## Validation Rules (Match Backend)

Before sending, validate:

```typescript
interface ValidationRules {
  latitude: { min: -90, max: 90, required: true };
  longitude: { min: -180, max: 180, required: true };
  bearing: { min: 0, max: 360, required: false };
  speed: { min: 0, max: 200, required: false }; // m/s
  vehicleId: { pattern: /^[a-zA-Z0-9_-]+$/, required: true };
  feedId: { pattern: /^[a-z]{2,10}$/, required: true };
  agencyId: { pattern: /^[a-z0-9_-]+$/, required: true };
}
```

---

## Expected File Structure

```
src/
├── app/
│   ├── services/
│   │   ├── location.service.ts
│   │   ├── protocol.service.ts
│   │   ├── transport.service.ts
│   │   ├── config.service.ts
│   │   └── offline-queue.service.ts
│   ├── models/
│   │   ├── vehicle-position.model.ts
│   │   └── config.model.ts
│   ├── pages/
│   │   ├── home/          # Main tracking screen
│   │   ├── config/        # Settings screen
│   │   └── testing/       # Testing mode screen
│   └── proto/
│       └── gtfs-realtime.js  # Compiled protobuf
├── assets/
│   └── proto/
│       └── gtfs-realtime.proto
```

---

## Integration Test Checklist

Before declaring integration complete, verify:

- [ ] REST+Protobuf sends successfully, backend logs show parsed data
- [ ] REST+JSON sends successfully, backend logs show parsed data
- [ ] MQTT+Protobuf publishes, backend Kafka consumer receives message
- [ ] Invalid data is rejected with appropriate error
- [ ] Offline queue works (disable network, queue fills, re-enable, queue drains)
- [ ] Background tracking continues when app is backgrounded
- [ ] MQTT reconnects after broker restart

---

## Backend Endpoints (For Reference)

| Pathway | URL | Port | Protocol |
|---------|-----|------|----------|
| REST Proto | `http://{host}:8087/vp/f/{feedId}/a/{agencyId}` | 8087 | HTTP |
| REST JSON | `http://{host}:8087/vp/f/{feedId}/r/{routeId}/t/{tripId}/d/{direction}` | 8087 | HTTP |
| MQTT | `mqtt://{host}:1883` | 1883 | MQTT |

---

## Notes for Minimal Friction

1. **Use the same protobuf schema** - Download `gtfs-realtime.proto` from GTFS-RT spec
2. **MQTT topic structure is strict** - Backend parses by position (Digitransit format)
3. **Timestamps are Unix epoch seconds** - Not milliseconds
4. **Coordinates are floats** - Not strings
5. **Test with backend test scripts first** - `scripts/mqtt_test_publisher.py`

---

Start by implementing the **Protocol Service** with protobuf support, then add the **Transport Service** with REST+Protobuf. This establishes the core contract. Add JSON and MQTT afterward.


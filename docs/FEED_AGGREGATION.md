# GTFS-RT Aggregated Feed Endpoint Implementation

## Overview

Complete implementation of GTFS-RT feed aggregation endpoints for serving vehicle position data to transit consumers like Google Transit. Supports both **binary protobuf** (production) and **ASCII text format** (development/debug) as per GTFS-RT specification.

## Architecture

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Transit   │ POST    │  VPService  │  Cache  │    Redis    │
│  Provider   │────────▶│             │────────▶│   vp:*      │
└─────────────┘         └─────────────┘         └─────────────┘
                              │                         │
                              │ Kafka                   │ Read
                              ▼                         ▼
                        ┌─────────────┐         ┌─────────────┐
                        │   Kafka     │         │ FeedService │
                        │   Topic     │         │ (Aggregate) │
                        └─────────────┘         └─────────────┘
                                                       │
                                                       │ GET
                                                       ▼
                                                ┌─────────────┐
                                                │   Google    │
                                                │  Transit    │
                                                └─────────────┘
```

## Implemented Components

### 1. **FeedService** (`service/FeedService.java`)
   - Aggregates vehicle positions from Redis
   - Builds complete `FeedMessage` with header and entities
   - Supports filtering by feedId and agencyId
   - Tracks last modified timestamp for caching

### 2. **FeedController** (`controller/FeedController.java`)
   - **GET `/gtfs-rt/feed.pb`** - Serves aggregated feed
   - **GET `/gtfs-rt/status`** - Health check endpoint
   - Supports HTTP 304 Not Modified for efficient caching
   - Content negotiation for binary/text format

### 3. **VPConverter Enhancement** (`converter/VPConverter.java`)
   - **`mapFromFeedEntity()`** - Converts GTFS-RT protobuf → domain entity
   - **`entityToFeedEntity()`** - Converts domain entity → GTFS-RT protobuf

### 4. **VPService Enhancement** (`service/VPService.java`)
   - Sends vehicle positions to Kafka
   - Caches in Redis with configurable TTL
   - Key pattern: `vp:{vehicleId}`

### 5. **Configuration** (`application.properties`)
```properties
# GTFS-RT Feed Configuration
gtfs.feed.version=2.0
gtfs.feed.incrementality=FULL_DATASET
gtfs.feed.cache.ttl-seconds=30
```

## API Endpoints

### POST - Receive Vehicle Position

#### Binary Protobuf (Production)
```bash
POST /vp/f/{feedId}/a/{agencyId}
Content-Type: application/x-protobuf

<binary FeedEntity data>
```

#### ASCII Text (Development/Debug)
```bash
POST /vp/f/{feedId}/a/{agencyId}
Content-Type: text/plain

id: "vehicle_001"
vehicle {
  trip { trip_id: "trip_123" }
  position { latitude: 52.23 longitude: 21.01 }
  timestamp: 1736432000
}
```

### GET - Serve Aggregated Feed

#### Binary Protobuf (Production)
```bash
GET /gtfs-rt/feed.pb
Accept: application/x-protobuf
```

Returns: Complete `FeedMessage` in binary protobuf format

#### ASCII Text (Development/Debug)
```bash
GET /gtfs-rt/feed.pb
Accept: text/plain
```

Returns: Human-readable text representation

#### With Filters
```bash
GET /gtfs-rt/feed.pb?feedId=ztp-feed&agencyId=ztp-agency
```

#### With Caching
```bash
GET /gtfs-rt/feed.pb
If-Modified-Since: 1736432000
```

Returns: 304 Not Modified if no updates

### GET - Feed Status
```bash
GET /gtfs-rt/status
```

Returns: `GTFS-RT feed active. Last update: 30 seconds ago`

## Response Headers

All feed responses include:
- `Content-Type`: `application/x-protobuf` or `text/plain;charset=UTF-8`
- `Last-Modified`: Timestamp of most recent vehicle position
- `Cache-Control`: `max-age=30` (configurable via `gtfs.feed.cache.ttl-seconds`)

## Testing

### Integration Tests

**`VPControllerProtoTest.java`** - Binary protobuf POST endpoint
- Valid FeedEntity → 200 OK
- Missing vehicle data → 400 Bad Request
- Complete GTFS-RT data → 200 OK

**`FeedControllerTextFormatTest.java`** - Text format support
- POST with text format → 200 OK
- GET with text format → readable output
- GET with binary format → valid protobuf
- Content negotiation → defaults to binary

### Manual Testing Scripts

#### Python Script
```bash
# Binary protobuf format
python test_proto_vp.py

# ASCII text format (debug)
python test_proto_vp.py --text
```

#### Batch Scripts
```bash
# Test binary protobuf
test-proto-vp.bat

# Test ASCII text format
test-text-format.bat
```

#### cURL Examples

**Send vehicle position (binary)**:
```bash
curl -X POST http://localhost:8087/vp/f/ztp-feed/a/ztp-agency \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @test_vehicle.pb
```

**Send vehicle position (text)**:
```bash
curl -X POST http://localhost:8087/vp/f/ztp-feed/a/ztp-agency \
  -H "Content-Type: text/plain" \
  --data-binary @test_vehicle.txt
```

**Get feed (binary)**:
```bash
curl -X GET http://localhost:8087/gtfs-rt/feed.pb \
  -H "Accept: application/x-protobuf" \
  --output feed.pb
```

**Get feed (text - for debugging)**:
```bash
curl -X GET http://localhost:8087/gtfs-rt/feed.pb \
  -H "Accept: text/plain"
```

**Get feed with caching**:
```bash
curl -X GET http://localhost:8087/gtfs-rt/feed.pb \
  -H "If-Modified-Since: 1736432000" \
  -v
```

## Data Flow

### 1. Ingestion (POST)
```
Transit Provider → POST /vp/f/{feedId}/a/{agencyId}
                 → VPController.sendVPProto()
                 → VPConverter.mapFromFeedEntity()
                 → VPService.sendToKafka()
                 → Kafka Topic + Redis Cache
```

### 2. Aggregation (GET)
```
Google Transit → GET /gtfs-rt/feed.pb
              → FeedController.getVehiclePositionFeed()
              → FeedService.buildVehiclePositionFeed()
              → Read from Redis (vp:*)
              → VPConverter.entityToFeedEntity()
              → Build FeedMessage
              → Return binary or text
```

## FeedMessage Structure

```protobuf
FeedMessage {
  header {
    gtfs_realtime_version: "2.0"
    timestamp: 1736432000
    incrementality: FULL_DATASET
  }
  entity [
    {
      id: "vehicle_001"
      vehicle {
        trip { trip_id: "trip_123" route_id: "route_67" ... }
        position { latitude: 52.23 longitude: 21.01 }
        timestamp: 1736432000
        stop_id: "stop_001"
        current_status: STOPPED_AT
        occupancy_status: FEW_SEATS_AVAILABLE
      }
    },
    {
      id: "vehicle_002"
      vehicle { ... }
    }
    ...
  ]
}
```

## Caching Strategy

### Redis Cache
- **Key Pattern**: `vp:{vehicleId}`
- **TTL**: `cacheTtlSeconds * 2` (default: 60 seconds)
- **Purpose**: Fast feed aggregation without database queries

### HTTP Cache-Control
- **max-age**: `cacheTtlSeconds` (default: 30 seconds)
- **Purpose**: Reduce server load from frequent polling
- **304 Support**: Returns Not Modified when data hasn't changed

## Performance Considerations

1. **Redis Lookup**: O(n) where n = number of vehicles
2. **Feed Build**: Single pass conversion
3. **HTTP Caching**: Reduces server hits by ~50-80%
4. **Binary Format**: ~3-5x smaller than text format

## Google Transit Integration

Google Transit will poll this endpoint every 30-60 seconds:

```bash
GET http://your-server:8087/gtfs-rt/feed.pb
Accept: application/x-protobuf
If-Modified-Since: {last-fetch-timestamp}
```

Your server responds:
- **200 OK** with updated feed
- **304 Not Modified** if no changes

## Development Workflow

### 1. Development (Text Format)
```bash
# Send test data
curl -X POST http://localhost:8087/vp/f/test/a/test \
  -H "Content-Type: text/plain" \
  --data-binary @test_vehicle.txt

# View feed in readable format
curl http://localhost:8087/gtfs-rt/feed.pb -H "Accept: text/plain"
```

### 2. Production (Binary Format)
```bash
# Transit provider sends binary
POST /vp/f/prod-feed/a/prod-agency
Content-Type: application/x-protobuf

# Google Transit receives binary
GET /gtfs-rt/feed.pb
Accept: application/x-protobuf
```

## Configuration Options

```properties
# Feed version (GTFS-RT spec version)
gtfs.feed.version=2.0

# Incrementality: FULL_DATASET or DIFFERENTIAL
gtfs.feed.incrementality=FULL_DATASET

# Cache TTL in seconds (affects both Redis and HTTP)
gtfs.feed.cache.ttl-seconds=30
```

## Error Handling

| Scenario | Status Code | Response |
|----------|-------------|----------|
| Valid request | 200 OK | Feed data |
| No changes | 304 Not Modified | Empty body |
| Invalid format | 400 Bad Request | Error message |
| Server error | 500 Internal Server Error | Error message |

## Monitoring

### Health Check
```bash
GET /gtfs-rt/status
```

Returns feed age and status.

### Logs
- INFO: Feed requests and entity counts
- DEBUG: Redis operations and format selection
- ERROR: Parsing failures and cache errors

## Files Created/Modified

### Created:
- `service/FeedService.java` - Feed aggregation logic
- `controller/FeedController.java` - GET endpoints
- `test/FeedControllerTextFormatTest.java` - Text format tests
- `test_vehicle.txt` - ASCII text sample
- `test-text-format.bat` - Text format test script
- `FEED_AGGREGATION.md` - This documentation

### Modified:
- `controller/VPController.java` - Added text format support
- `converter/VPConverter.java` - Added reverse conversion
- `service/VPService.java` - Added Redis caching
- `test_proto_vp.py` - Added text format support
- `application.properties` - Added feed configuration

## Next Steps

1. Deploy to production server
2. Configure Google Transit partner portal with endpoint URL
3. Monitor feed requests and cache hit ratio
4. Adjust `gtfs.feed.cache.ttl-seconds` based on update frequency
5. Add metrics/monitoring for feed availability

## References

- [GTFS-RT Specification](https://support.google.com/transitpartners/answer/9047739)
- [Google Transit Partner Portal](https://developers.google.com/transit)
- [Protobuf Text Format](https://protobuf.dev/reference/protobuf/textformat-spec/)


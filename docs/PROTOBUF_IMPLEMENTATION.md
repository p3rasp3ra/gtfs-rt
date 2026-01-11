# GTFS-RT Protobuf Endpoint Implementation

## Summary

Successfully implemented protobuf support for the Vehicle Position REST endpoint.

## What Was Implemented

### 1. **Controller Endpoint** (`VPController.java`)
   - **Path**: `POST /vp/f/{feedId}/a/{agencyId}`
   - **Content-Type**: `application/x-protobuf` (vs `application/json` for the other endpoint)
   - **Input**: GTFS-RT `FeedEntity` containing `VehiclePosition`
   - **Functionality**: Same as JSON endpoint - sends to Kafka

### 2. **Converter Method** (`VPConverter.mapFromFeedEntity`)
   - Converts GTFS-RT `FeedEntity` to domain `VehiclePosition`
   - Extracts all GTFS-RT fields:
     - Trip info (trip_id, route_id, direction_id, start_date, start_time)
     - Position (latitude, longitude)
     - Timestamp (converts from Unix epoch to Instant)
     - Stop info (stop_id, current_status)
     - Occupancy status
   - Uses FeedEntity ID as vehicle ID
   - Returns `null` if FeedEntity doesn't contain vehicle position

### 3. **Spring Configuration** (`WebConfig.java`)
   - Registered `ProtobufHttpMessageConverter` bean
   - Enables automatic protobuf deserialization

### 4. **Integration Tests** (`VPControllerProtoTest.java`)
   - ✅ Valid vehicle position data → 200 OK
   - ✅ Minimal required fields → 200 OK
   - ✅ Missing vehicle data → 400 Bad Request
   - ✅ TripUpdate instead of VehiclePosition → 400 Bad Request
   - ✅ Complete GTFS-RT data with all fields → 200 OK

### 5. **Testing Tools**

#### **Python Script** (`scripts/test_proto_vp.py`)
```bash
# Install dependencies
pip install gtfs-realtime-bindings requests

# Run test
python scripts\test_proto_vp.py
```

#### **Java Test File Generator** (`ProtoTestFileGenerator.java`)
```bash
# Generate test protobuf file
cd src/test/java
javac -cp .:../../../build/libs/* com/marszrut/gtfs_rt/util/ProtoTestFileGenerator.java
java -cp .:../../../build/libs/* com.marszrut.gtfs_rt.util.ProtoTestFileGenerator
```

#### **cURL Script** (`scripts/test-proto-vp.bat`)
```bash
# After generating test_vehicle.pb with Java or Python
scripts\test-proto-vp.bat
```

## Usage Examples

### Python
```python
from google.transit import gtfs_realtime_pb2
import requests

entity = gtfs_realtime_pb2.FeedEntity()
entity.id = "vehicle_123"
entity.vehicle.position.latitude = 52.2297
entity.vehicle.position.longitude = 21.0122
entity.vehicle.timestamp = int(time.time())

response = requests.post(
    'http://localhost:8087/vp/f/ztp-feed/a/ztp-agency',
    headers={'Content-Type': 'application/x-protobuf'},
    data=entity.SerializeToString()
)
```

### cURL
```bash
curl -X POST http://localhost:8087/vp/f/ztp-feed/a/ztp-agency \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @test_vehicle.pb
```

### Java/Spring RestClient
```java
GtfsRealtime.FeedEntity entity = GtfsRealtime.FeedEntity.newBuilder()
    .setId("vehicle_123")
    .setVehicle(...)
    .build();

RestClient restClient = RestClient.create();
restClient.post()
    .uri("http://localhost:8087/vp/f/ztp-feed/a/ztp-agency")
    .contentType(MediaType.parseMediaType("application/x-protobuf"))
    .body(entity.toByteArray())
    .retrieve()
    .toBodilessEntity();
```

## API Design

The endpoint uses **content negotiation** to differentiate between JSON and protobuf:

| Content-Type | Path | Input Format |
|-------------|------|-------------|
| `application/json` | `/vp/f/{feedId}/r/{routeId}/t/{tripId}/d/{direction}` | JSON DTO |
| `application/x-protobuf` | `/vp/f/{feedId}/a/{agencyId}` | GTFS-RT FeedEntity |

Different path parameters because:
- JSON version requires explicit route/trip/direction in URL
- Protobuf version extracts these from FeedEntity itself

## Testing Status

✅ **All integration tests passing**
✅ **Build successful**
✅ **No compilation errors**
✅ **Compatible with Spring Boot 4.0**

## Files Modified/Created

### Modified:
- `VPController.java` - Added `sendVPProto` endpoint
- `VPConverter.java` - Added `mapFromFeedEntity` method

### Created:
- `WebConfig.java` - Protobuf HTTP message converter configuration
- `VPControllerProtoTest.java` - Integration tests
- `ProtoTestFileGenerator.java` - Test file generator utility
- `test_proto_vp.py` - Python test script
- `test-proto-vp.bat` - cURL test script
- `PROTOBUF_IMPLEMENTATION.md` - This documentation

## Next Steps

1. Run the application: `.\gradlew.bat bootRun`
2. Generate test file: Run `ProtoTestFileGenerator.java` or `test_proto_vp.py`
3. Test endpoint: Use cURL script or Python script
4. Monitor Kafka topic to verify messages are being sent

## Notes

- Vehicle label and license plate fields are empty in proto conversion (not in GTFS-RT spec)
- These can be enriched from a separate vehicle registry if needed
- FeedEntity ID is used as vehicle ID per GTFS-RT spec


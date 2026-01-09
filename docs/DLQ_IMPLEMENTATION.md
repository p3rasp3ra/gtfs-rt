# Separate DLQ Implementation for Slow Path - Summary

## What Was Implemented

### 1. **Configuration Updates** (`application.properties`)
- Replaced single DLQ topic with **separate DLQ topics per consumer group**:
  - `kafka.topics.vehicle-positions.slow-path-dlq=gtfsrt.vp.slow-path.DLT`
  - `kafka.topics.vehicle-positions.fast-path-dlq=gtfsrt.vp.fast-path.DLT`

### 2. **Kafka Configuration** (`KafkaConfig.java`)
- Created **`slowPathKafkaListenerContainerFactory`**:
  - Dedicated factory for slow-path consumers (DB persistence)
  - Routes failures to `gtfsrt.vp.slow-path.DLT`
  - Retry policy: 3 retries with 5 seconds between each
- Updated **`kafkaListenerContainerFactory`** (fast-path):
  - Routes failures to `gtfsrt.vp.fast-path.DLT`
  - Retry policy: 2 retries with 1 second between each

### 3. **Listener Update** (`JsonVpSlowPathListener.java`)
- Changed `containerFactory` from `kafkaListenerContainerFactory` to **`slowPathKafkaListenerContainerFactory`**
- Now uses dedicated slow-path factory with its own DLQ

### 4. **DLQ Monitor** (`DlqMonitor.java` - NEW)
- Monitors both DLQ topics for failed messages
- Logs detailed failure information for debugging
- Separate handlers for slow-path (DB) vs fast-path (Redis) failures
- Ready for integration with alerting systems (Slack, PagerDuty, etc.)

### 5. **DLQ Replay Service** (`SlowPathDlqReplayService.java` - NEW)
- Allows replaying failed messages from slow-path DLQ back to main topic
- **Disabled by default** (`autoStartup = false`)
- Enable when needed via property: `dlq.replay.slow-path.enabled=true`
- Use after fixing root cause (e.g., after applying Flyway migration)

---

## Current Error & Solution

### The Error
```
ERROR: column "direction_id" of relation "vehicle_positions" does not exist
```

### Root Cause
Your `VehiclePosition` entity has a `directionId` field, but the database table `vehicle_positions` is missing the `direction_id` column.

### Solution Steps

#### Step 1: Create Flyway Migration
Create file: `src/main/resources/db/migration/V2__add_direction_id_to_vehicle_positions.sql`

```sql
-- Add missing direction_id column to vehicle_positions table
ALTER TABLE vehicle_positions
ADD COLUMN direction_id INTEGER;

-- Add comment for documentation
COMMENT ON COLUMN vehicle_positions.direction_id IS 'Trip direction: 0 for outbound, 1 for inbound';
```

#### Step 2: Apply Migration
Restart the application. Flyway will automatically apply the migration.

#### Step 3: Verify Messages in DLQ
Check the slow-path DLQ topic for failed messages:
```bash
# Using Kafka console consumer
kafka-console-consumer --bootstrap-server localhost:9093 \
  --topic gtfsrt.vp.slow-path.DLT \
  --from-beginning
```

#### Step 4: Replay Failed Messages (Optional)
After migration is applied, enable DLQ replay:

Add to `application.properties`:
```properties
# Enable DLQ replay to reprocess failed messages
dlq.replay.slow-path.enabled=true
```

Restart the application. Messages from DLQ will be replayed to the main topic and successfully persisted.

After replay completes, disable:
```properties
dlq.replay.slow-path.enabled=false
```

---

## Benefits of This Implementation

1. **Isolated Failure Handling**
   - Slow-path (DB) failures don't affect fast-path (Redis) processing
   - Each consumer type has its own DLQ and retry policy

2. **Targeted Monitoring**
   - Know immediately whether failure is DB-related or cache-related
   - Different alert severity/routing per DLQ type

3. **Independent Replay**
   - Replay only slow-path failures without touching fast-path
   - Fast-path continues working during slow-path issues

4. **Better Debugging**
   - Clear separation of concerns
   - Logs clearly indicate which path failed
   - DLQ monitor provides detailed failure context

5. **Scalability**
   - Can scale slow-path and fast-path independently
   - Different teams can own different DLQs
   - Different retry/timeout policies per consumer type

---

## DLQ Message Flow

### Normal Flow
```
Producer (VPService)
    ↓
kafka topic: gtfsrt.vp.raw
    ↓
    ├─→ Fast Path Consumer (Redis updates)
    │   └─→ Success or → gtfsrt.vp.fast-path.DLT
    │
    └─→ Slow Path Consumer (DB persistence)
        └─→ Success or → gtfsrt.vp.slow-path.DLT
```

### After Failure (Current State)
```
gtfsrt.vp.slow-path.DLT
    ↓
DlqMonitor (logs & alerts)
    ↓
Fix root cause (apply Flyway migration)
    ↓
Enable SlowPathDlqReplayService
    ↓
Messages replayed to gtfsrt.vp.raw
    ↓
JsonVpSlowPathListener (successful persistence)
```

---

## Files Modified/Created

### Modified
1. `src/main/resources/application.properties`
2. `src/main/java/com/marszrut/gtfs_rt/config/KafkaConfig.java`
3. `src/main/java/com/marszrut/gtfs_rt/ingestion/JsonVpSlowPathListener.java`

### Created
1. `src/main/java/com/marszrut/gtfs_rt/monitoring/DlqMonitor.java`
2. `src/main/java/com/marszrut/gtfs_rt/replay/SlowPathDlqReplayService.java`

---

## Next Steps

1. ✅ **Separate DLQ implemented** (DONE)
2. ⏳ **Create Flyway migration** for `direction_id` column
3. ⏳ **Apply migration** (restart application)
4. ⏳ **Verify** slow-path consumer works
5. ⏳ **Replay DLQ messages** (if any failed before fix)

---

## Monitoring Commands

### Check DLQ Messages
```bash
# Slow path DLQ
kafka-console-consumer --bootstrap-server localhost:9093 \
  --topic gtfsrt.vp.slow-path.DLT \
  --from-beginning \
  --property print.key=true \
  --property key.separator=":"

# Fast path DLQ
kafka-console-consumer --bootstrap-server localhost:9093 \
  --topic gtfsrt.vp.fast-path.DLT \
  --from-beginning \
  --property print.key=true \
  --property key.separator=":"
```

### Check Consumer Lag
```bash
kafka-consumer-groups --bootstrap-server localhost:9093 \
  --group vp-json-slow-path-group \
  --describe
```

---

## Future Enhancements

1. **Alerting Integration**
   - Add Slack/email notifications in `DlqMonitor`
   - Set up PagerDuty for critical slow-path failures

2. **Metrics**
   - Add Micrometer counters for DLQ messages
   - Dashboard for DLQ message counts over time

3. **Automated Replay**
   - Detect when root cause is fixed
   - Auto-enable replay service
   - Auto-disable after DLQ is empty

4. **DLQ Archival**
   - Move old DLQ messages to long-term storage
   - Prevent DLQ topics from growing unbounded


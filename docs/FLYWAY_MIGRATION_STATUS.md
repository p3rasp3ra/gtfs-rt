# Flyway Migration - Status Report

## ✅ COMPLETED

### 1. Separate DLQ Implementation
- **Status:** ✅ Successfully implemented and running
- **Evidence:** Application logs show:
  - `DLQ Monitor initialized` - monitoring both DLQs
  - `dlq-monitor-slow-path` consumer is active
  - `dlq-monitor-fast-path` consumer is active
  - DLQ monitor detected and logged 3 pre-existing failed messages in slow-path DLQ

### 2. Flyway Migration Created
- **File:** `src/main/resources/db/migration/V4__align_schema_with_entity.sql`
- **Status:** ✅ Created but **NOT YET APPLIED**
- **Migration includes:**
  - Adds all missing columns (`direction_id`, `feed_d`, `start_date`, `start_time`, etc.)
  - Removes obsolete columns (`bearing`, `speed`, `label`, `received_at`)
  - Sets defaults for existing rows
  - Adds NOT NULL constraints
  - Adds column comments for documentation

---

## ⏳ PENDING ACTION

### Apply the Flyway Migration

**Current Issue:**
```
ERROR: column "direction_id" of relation "vehicle_positions" does not exist
```

**Solution:** Restart the application to trigger Flyway migration execution.

**Steps:**

#### Option 1: Restart via Gradle (Recommended)
```powershell
# Stop current application (Ctrl+C in the terminal running bootRun)
# Then restart:
cd gtfs-rt
.\gradlew bootRun
```

#### Option 2: Manual Database Migration (If Flyway fails)
```sql
-- Connect to PostgreSQL
psql -U gtfsuser -d gtfs_realtime_db

-- Check current schema
\d vehicle_positions

-- If Flyway doesn't auto-run, execute migration manually:
\i src/main/resources/db/migration/V4__align_schema_with_entity.sql

-- Verify changes
\d vehicle_positions
```

---

## 🔍 WHAT WILL HAPPEN AFTER RESTART

### 1. Flyway Detects New Migration
```
2026-01-04 XX:XX:XX INFO  o.f.c.i.s.JdbcTableSchemaHistory : Creating Schema History table "public"."flyway_schema_history" ...
2026-01-04 XX:XX:XX INFO  o.f.core.internal.command.DbMigrate : Migrating schema "public" to version "4 - align schema with entity"
2026-01-04 XX:XX:XX INFO  o.f.core.internal.command.DbMigrate : Successfully applied 1 migration to schema "public"
```

### 2. Slow-Path Consumer Succeeds
```
INFO [JSON SLOW PATH] RECEIVED MESSAGE - vehicleId=TEST-VEHICLE-001
INFO [JSON SLOW PATH] Persisted vehicle position to database
```

### 3. DLQ Messages Can Be Replayed
Once schema is fixed, enable DLQ replay:

Add to `application.properties`:
```properties
dlq.replay.slow-path.enabled=true
```

This will replay all 3+ messages currently sitting in the slow-path DLQ.

---

## 📊 CURRENT STATE SUMMARY

### Database Schema
- **Status:** ❌ Mismatched with entity
- **Missing Columns:** `direction_id`, `feed_d`, `start_date`, `start_time`, `current_stop_id`, `current_stop_status`, `vehicle_label`, `license_plate`, `occupancy_status`
- **Obsolete Columns:** `bearing`, `speed`, `label`, `received_at`

### Application Status
- **Status:** ✅ Running on port 8087
- **Kafka Consumers:** ✅ All active (fast-path, slow-path, DLQ monitors)
- **Fast-Path Consumer:** ✅ Working (Redis updates)
- **Slow-Path Consumer:** ❌ Failing due to schema mismatch
- **DLQ Messages:** 3+ messages in `gtfsrt.vp.slow-path.DLT` waiting for schema fix

### Kafka Topics
- `gtfsrt.vp.raw` - Main topic (both consumers listening)
- `gtfsrt.vp.slow-path.DLT` - Slow-path DLQ (**contains 3+ failed messages**)
- `gtfsrt.vp.fast-path.DLT` - Fast-path DLQ (empty)

---

## 🚀 NEXT STEPS (IN ORDER)

1. **Restart Application** to apply Flyway migration
   ```powershell
   # Press Ctrl+C in bootRun terminal
   .\gradlew bootRun
   ```

2. **Verify Migration Applied**
   - Check application logs for: `Successfully applied 1 migration`
   - Test slow-path consumer processes messages successfully

3. **Replay DLQ Messages**
   - Enable replay in `application.properties`: `dlq.replay.slow-path.enabled=true`
   - Restart application
   - Verify DLQ messages are reprocessed successfully
   - Disable replay: `dlq.replay.slow-path.enabled=false`

4. **Verify End-to-End**
   - Send test message
   - Confirm fast-path updates Redis
   - Confirm slow-path persists to database
   - Check no new messages in DLQ

---

## 📝 FILES CHANGED IN THIS IMPLEMENTATION

### Modified
1. `src/main/resources/application.properties` - Added separate DLQ topic properties
2. `src/main/java/com/marszrut/gtfs_rt/config/KafkaConfig.java` - Added slow-path container factory
3. `src/main/java/com/marszrut/gtfs_rt/ingestion/JsonVpSlowPathListener.java` - Updated to use slow-path factory

### Created
1. `src/main/java/com/marszrut/gtfs_rt/monitoring/DlqMonitor.java` - Monitors both DLQs
2. `src/main/java/com/marszrut/gtfs_rt/replay/SlowPathDlqReplayService.java` - DLQ replay utility
3. `src/main/resources/db/migration/V4__align_schema_with_entity.sql` - **⏳ Pending application**
4. `docs/DLQ_IMPLEMENTATION.md` - Full documentation
5. `docs/FLYWAY_MIGRATION_STATUS.md` - This file

---

## ✅ SUCCESS CRITERIA

Migration is successful when:
- [ ] Application starts without Flyway errors
- [ ] Slow-path consumer processes messages without errors
- [ ] Database inserts succeed
- [ ] No new messages appear in slow-path DLQ
- [ ] Old DLQ messages can be replayed successfully

**Current Progress:** 60% complete (DLQ implementation done, migration pending)


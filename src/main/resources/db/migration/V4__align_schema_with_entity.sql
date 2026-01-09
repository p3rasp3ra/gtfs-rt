-- Migration to align vehicle_positions table with VehiclePosition entity
-- Adds missing columns and removes obsolete ones to match the domain model

-- Add missing columns from entity (one by one for PostgreSQL compatibility)
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS feed_id VARCHAR(50);
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS direction_id INTEGER;
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS start_date VARCHAR(10);
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS start_time VARCHAR(10);
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS current_stop_id VARCHAR(50);
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS current_stop_status INTEGER;
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS vehicle_label VARCHAR(100);
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS license_plate VARCHAR(50);
ALTER TABLE vehicle_positions ADD COLUMN IF NOT EXISTS occupancy_status INTEGER;

-- Drop obsolete columns that don't exist in entity
ALTER TABLE vehicle_positions DROP COLUMN IF EXISTS bearing;
ALTER TABLE vehicle_positions DROP COLUMN IF EXISTS speed;
ALTER TABLE vehicle_positions DROP COLUMN IF EXISTS label;
ALTER TABLE vehicle_positions DROP COLUMN IF EXISTS received_at;

-- Set defaults for existing rows (if any exist)
UPDATE vehicle_positions
SET
    feed_id = COALESCE(feed_id, 'unknown'),
    agency_id = COALESCE(agency_id, 'unknown'),
    route_id = COALESCE(route_id, 'unknown'),
    trip_id = COALESCE(trip_id, 'unknown'),
    direction_id = COALESCE(direction_id, 0),
    start_date = COALESCE(start_date, '19700101'),
    start_time = COALESCE(start_time, '00:00:00'),
    current_stop_id = COALESCE(current_stop_id, 'unknown'),
    current_stop_status = COALESCE(current_stop_status, 0),
    vehicle_label = COALESCE(vehicle_label, 'unknown'),
    license_plate = COALESCE(license_plate, 'unknown'),
    occupancy_status = COALESCE(occupancy_status, 0)
WHERE
    feed_id IS NULL OR
    agency_id IS NULL OR
    route_id IS NULL OR
    trip_id IS NULL OR
    direction_id IS NULL OR
    start_date IS NULL OR
    start_time IS NULL OR
    current_stop_id IS NULL OR
    current_stop_status IS NULL OR
    vehicle_label IS NULL OR
    license_plate IS NULL OR
    occupancy_status IS NULL;

-- Add NOT NULL constraints (one by one)
ALTER TABLE vehicle_positions ALTER COLUMN feed_id SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN agency_id SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN route_id SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN trip_id SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN direction_id SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN start_date SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN start_time SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN current_stop_id SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN current_stop_status SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN vehicle_label SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN license_plate SET NOT NULL;
ALTER TABLE vehicle_positions ALTER COLUMN occupancy_status SET NOT NULL;

-- Add column comments for documentation
COMMENT ON COLUMN vehicle_positions.feed_id IS 'Feed ID identifying the data source';
COMMENT ON COLUMN vehicle_positions.direction_id IS 'Trip direction: 0 for outbound, 1 for inbound';
COMMENT ON COLUMN vehicle_positions.start_date IS 'Trip start date in YYYYMMDD format';
COMMENT ON COLUMN vehicle_positions.start_time IS 'Trip start time in HH:MM:SS format';
COMMENT ON COLUMN vehicle_positions.current_stop_id IS 'Current or next stop identifier';
COMMENT ON COLUMN vehicle_positions.current_stop_status IS 'Stop status: 0=in transit to, 1=stopped at';
COMMENT ON COLUMN vehicle_positions.vehicle_label IS 'Human-readable vehicle label';
COMMENT ON COLUMN vehicle_positions.license_plate IS 'Vehicle license plate number';
COMMENT ON COLUMN vehicle_positions.occupancy_status IS 'Vehicle occupancy status code';


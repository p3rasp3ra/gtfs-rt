-- Initial schema: Create vehicle_positions table for GTFS-RT data
-- This migration creates the complete table structure matching the VehiclePosition entity

CREATE TABLE IF NOT EXISTS vehicle_positions (
    id BIGSERIAL,
    feed_id VARCHAR(50) NOT NULL,
    vehicle_id VARCHAR(50) NOT NULL,
    agency_id VARCHAR(50) NOT NULL,
    route_id VARCHAR(50) NOT NULL,
    trip_id VARCHAR(100) NOT NULL,
    direction_id INTEGER NOT NULL,
    start_date VARCHAR(10) NOT NULL,
    start_time VARCHAR(10) NOT NULL,
    current_stop_id VARCHAR(50) NOT NULL,
    current_stop_status INTEGER NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    vehicle_label VARCHAR(100) NOT NULL,
    license_plate VARCHAR(50) NOT NULL,
    occupancy_status INTEGER NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, timestamp)
);

-- Add table and column comments
COMMENT ON TABLE vehicle_positions IS 'Time-series data for GTFS-RT vehicle positions';
COMMENT ON COLUMN vehicle_positions.feed_id IS 'Feed ID identifying the data source';
COMMENT ON COLUMN vehicle_positions.vehicle_id IS 'Unique vehicle identifier';
COMMENT ON COLUMN vehicle_positions.agency_id IS 'Transit agency identifier';
COMMENT ON COLUMN vehicle_positions.route_id IS 'Route identifier';
COMMENT ON COLUMN vehicle_positions.trip_id IS 'Trip identifier';
COMMENT ON COLUMN vehicle_positions.direction_id IS 'Trip direction: 0 for outbound, 1 for inbound';
COMMENT ON COLUMN vehicle_positions.start_date IS 'Trip start date in YYYYMMDD format';
COMMENT ON COLUMN vehicle_positions.start_time IS 'Trip start time in HH:MM:SS format';
COMMENT ON COLUMN vehicle_positions.current_stop_id IS 'Current or next stop identifier';
COMMENT ON COLUMN vehicle_positions.current_stop_status IS 'Stop status: 0=in transit to, 1=stopped at';
COMMENT ON COLUMN vehicle_positions.latitude IS 'Vehicle latitude coordinate';
COMMENT ON COLUMN vehicle_positions.longitude IS 'Vehicle longitude coordinate';
COMMENT ON COLUMN vehicle_positions.vehicle_label IS 'Human-readable vehicle label';
COMMENT ON COLUMN vehicle_positions.license_plate IS 'Vehicle license plate number';
COMMENT ON COLUMN vehicle_positions.occupancy_status IS 'Vehicle occupancy status code';
COMMENT ON COLUMN vehicle_positions.timestamp IS 'Timestamp from the GTFS-RT message';

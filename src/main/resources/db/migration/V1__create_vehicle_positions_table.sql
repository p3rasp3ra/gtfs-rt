-- Initial schema: Create vehicle_positions table
-- This migration creates the base table for storing GTFS-RT vehicle position data

CREATE TABLE IF NOT EXISTS vehicle_positions (
    id BIGSERIAL,
    vehicle_id VARCHAR(50) NOT NULL,
    agency_id VARCHAR(50),
    route_id VARCHAR(50),
    trip_id VARCHAR(100),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    bearing REAL,
    speed REAL,
    timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    label VARCHAR(100),
    PRIMARY KEY (id, timestamp)
);

-- Add table comment
COMMENT ON TABLE vehicle_positions IS 'Time-series data for GTFS-RT vehicle positions';
COMMENT ON COLUMN vehicle_positions.timestamp IS 'Timestamp from the GTFS-RT message';
COMMENT ON COLUMN vehicle_positions.received_at IS 'When the message was received and processed';


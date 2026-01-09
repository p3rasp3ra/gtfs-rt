-- Add indexes for efficient queries
-- This migration creates indexes optimized for common query patterns

-- Index for querying by vehicle_id and timestamp (most common query)
CREATE INDEX IF NOT EXISTS idx_vp_vehicle_id ON vehicle_positions (vehicle_id, timestamp DESC);

-- Index for querying by trip_id (only where trip_id is not null)
CREATE INDEX IF NOT EXISTS idx_vp_trip_id ON vehicle_positions (trip_id, timestamp DESC)
WHERE trip_id IS NOT NULL;

-- Index for querying by agency_id
CREATE INDEX IF NOT EXISTS idx_vp_agency_id ON vehicle_positions (agency_id, timestamp DESC)
WHERE agency_id IS NOT NULL;

-- Index for querying by received_at (for monitoring and debugging)
CREATE INDEX IF NOT EXISTS idx_vp_received_at ON vehicle_positions (received_at DESC);


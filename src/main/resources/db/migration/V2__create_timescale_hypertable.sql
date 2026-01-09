-- Convert to TimescaleDB hypertable
-- This migration enables time-series optimizations for vehicle_positions table

-- Convert the table to a TimescaleDB hypertable
-- Partitions data by timestamp with 1-day chunks
SELECT create_hypertable(
    'vehicle_positions',
    'timestamp',
    if_not_exists => TRUE,
    migrate_data => TRUE,
    chunk_time_interval => INTERVAL '1 day'
);


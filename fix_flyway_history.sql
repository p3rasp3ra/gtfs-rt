-- Fix Flyway schema history by manually recording V1, V2, V3 as already applied
-- Run this script directly in PostgreSQL if Flyway continues to fail

-- Delete any incorrect entries
DELETE FROM flyway_schema_history WHERE version IN ('1', '2', '3');

-- Insert baseline record for existing migrations
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script, checksum,
    installed_by, installed_on, execution_time, success
) VALUES
(1, '1', 'create vehicle positions table', 'SQL', 'V1__create_vehicle_positions_table.sql', NULL, 'gtfsuser', NOW(), 0, true),
(2, '2', 'create timescale hypertable', 'SQL', 'V2__create_timescale_hypertable.sql', NULL, 'gtfsuser', NOW(), 0, true),
(3, '3', 'add indexes', 'SQL', 'V3__add_indexes.sql', NULL, 'gtfsuser', NOW(), 0, true)
ON CONFLICT DO NOTHING;

-- Verify the entries
SELECT * FROM flyway_schema_history ORDER BY installed_rank;


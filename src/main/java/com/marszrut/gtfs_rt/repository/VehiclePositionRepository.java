package com.marszrut.gtfs_rt.repository;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for persisting Vehicle Position data to TimescaleDB.
 * TimescaleDB is a time-series database extension for PostgreSQL.
 */
@Repository
public interface VehiclePositionRepository extends JpaRepository<VehiclePosition, Long> {

    /**
     * Find recent positions for a specific vehicle.
     */
    List<VehiclePosition> findByVidAndTAfterOrderByTDesc(
            String vid, Instant after);

    /**
     * Find positions for a specific trip.
     */
    List<VehiclePosition> findByTidOrderByTDesc(String tid);
}


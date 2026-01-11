package com.marszrut.gtfs_rt.repository;

import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for persisting vehicle positions to db.
 */
@Repository
public interface VPRepository extends JpaRepository<VehiclePosition, Long> {
}


package com.marszrut.gtfs_rt.processing;

import com.marszrut.gtfs_rt.repository.VehiclePositionRepository;
import com.marszrut.gtfs_rt.domain.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service responsible for persisting vehicle position data to TimescaleDB.
 * This is the "slow path" - writes historical data for long-term storage and analysis.
 */
@Service
public class HistoryWriter {

    private static final Logger logger = LoggerFactory.getLogger(HistoryWriter.class);
    private final VehiclePositionRepository repository;


    HistoryWriter(VehiclePositionRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists a vehicle position to TimescaleDB.
     * Uses @Transactional to ensure data consistency.
     *
     * @param vp the enriched vehicle position domain object
     */
    @Transactional
    public void persistHistory(VehiclePosition vp) {
        try {
            repository.save(vp);
            repository.flush(); // Force immediate write to database
            logger.info("Persisted vehicle position to TimescaleDB: vehicleId={}, timestamp={}",
                    vp.getVid(), vp.getT());
        } catch (Exception e) {
            logger.error("Failed to persist vehicle position: vehicleId={}, error={}",
                    vp.getVid(), e.getMessage(), e);
            throw e;
        }
    }
}

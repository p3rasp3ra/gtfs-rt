package com.marszrut.gtfs_rt.exception;

/**
 * Exception thrown when data enrichment process fails.
 * This is typically used when deserialization of incoming data fails
 * or when data cannot be properly mapped to domain objects.
 */
public class DataEnrichmentException extends RuntimeException {

    public DataEnrichmentException(String message) {
        super(message);
    }

    public DataEnrichmentException(String message, Throwable cause) {
        super(message, cause);
    }
}

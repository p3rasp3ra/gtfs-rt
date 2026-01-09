package com.marszrut.gtfs_rt.exception;

/**
 * Exception thrown when validation of domain objects fails.
 * This is used when the data passes deserialization but contains invalid values
 * according to domain rules.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.repodna.exception;

/**
 * Thrown when directories or inputs fail validation checks.
 */
public class ValidationException extends RepoDnaException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

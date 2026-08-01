package com.repodna.exception;

/**
 * Base exception for all RepoDNA errors.
 */
public abstract class RepoDnaException extends RuntimeException {
    protected RepoDnaException(String message) {
        super(message);
    }

    protected RepoDnaException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.repodna.exception;

/**
 * Thrown when repository metadata, structures, or layouts are invalid or missing.
 */
public class RepositoryException extends RepoDnaException {
    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

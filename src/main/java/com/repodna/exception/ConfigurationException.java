package com.repodna.exception;

/**
 * Thrown when configuration loading, parsing, or validation fails.
 */
public class ConfigurationException extends RepoDnaException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

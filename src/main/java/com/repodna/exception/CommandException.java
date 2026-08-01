package com.repodna.exception;

/**
 * Thrown when command validation or execution fails.
 */
public class CommandException extends RepoDnaException {
    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}

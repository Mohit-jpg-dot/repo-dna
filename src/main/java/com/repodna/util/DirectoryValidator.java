package com.repodna.util;

import com.repodna.exception.ValidationException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates directory paths.
 */
public class DirectoryValidator {

    /**
     * Checks if directory exists, is a directory, and is readable.
     * Throws ValidationException if validation checks fail.
     */
    public static void validate(Path path) {
        if (path == null) {
            throw new ValidationException("Directory path cannot be null.");
        }
        if (!Files.exists(path)) {
            throw new ValidationException("Directory does not exist: " + path.toAbsolutePath());
        }
        if (!Files.isDirectory(path)) {
            throw new ValidationException("Path is not a directory: " + path.toAbsolutePath());
        }
        if (!Files.isReadable(path)) {
            throw new ValidationException("Directory is not readable: " + path.toAbsolutePath());
        }
    }
}

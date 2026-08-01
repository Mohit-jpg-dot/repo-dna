package com.repodna.scanner.model;

import java.util.List;

/**
 * Immutable representation of a framework detected in the repository.
 */
public record FrameworkInfo(
    String name,
    List<String> evidence
) {}

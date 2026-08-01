package com.repodna.discovery.model;

/**
 * Representation of a deterministic violation of an established engineering pattern.
 */
public record PatternViolation(
    String nodeId,
    String description,
    String snippet
) {}

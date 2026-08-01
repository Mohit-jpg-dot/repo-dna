package com.repodna.discovery.model;

/**
 * Immutable reference mapping a discovered pattern instance to its source location in the graph.
 */
public record PatternEvidence(
    String nodeId,
    String description,
    int line,
    String snippet
) {}

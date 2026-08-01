package com.repodna.discovery.model;

import java.util.List;

/**
 * Immutable model of an inferred codebase pattern with statistical confidence and location evidence.
 */
public record DiscoveredPattern(
    String id,
    PatternCategory category,
    String description,
    double confidence,
    int occurrences,
    int totalOpportunities,
    String reasoning,
    List<PatternEvidence> evidenceList,
    List<String> exceptionsList
) {}

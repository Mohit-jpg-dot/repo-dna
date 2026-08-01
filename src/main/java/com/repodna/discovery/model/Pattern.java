package com.repodna.discovery.model;

import java.util.List;

/**
 * Immutable representation of a discovered engineering pattern.
 */
public record Pattern(
    String id,
    PatternCategory category,
    String name,
    String description,
    PatternConfidence confidence,
    PatternHistory history,
    List<PatternEvidence> evidenceList,
    List<PatternViolation> violationsList
) {}

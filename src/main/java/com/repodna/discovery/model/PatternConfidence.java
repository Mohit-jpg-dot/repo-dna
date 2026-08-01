package com.repodna.discovery.model;

/**
 * Deterministic statistical confidence metrics for a pattern.
 */
public record PatternConfidence(
    double score,             // Statistical confidence score [0.0 - 1.0]
    int supportCount,         // Number of instances conforming to this pattern
    int violationCount,       // Number of instances violating this pattern
    double coverage,          // Portion of the codebase/candidates affected [0.0 - 1.0]
    double consistency        // Internal stability of the pattern
) {}

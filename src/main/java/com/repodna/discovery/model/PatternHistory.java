package com.repodna.discovery.model;

/**
 * Historical metadata for pattern evolution and classification.
 */
public record PatternHistory(
    int ageCommits,               // Duration pattern has been active in commits
    double stabilityScore,        // Consistency over time [0.0 - 1.0]
    PatternStability stability    // Classified stability enum
) {}

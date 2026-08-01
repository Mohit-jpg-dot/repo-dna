package com.repodna.evolution.model;

import java.util.List;

/**
 * Represents the results of the Genetic Distance Architecture Drift (GDAD) evaluation.
 */
public record EvolutionScore(
    double geneticDistance,
    boolean isDrifted,
    List<String> intentionalMigrations,
    List<String> accidentalDrifts
) {}

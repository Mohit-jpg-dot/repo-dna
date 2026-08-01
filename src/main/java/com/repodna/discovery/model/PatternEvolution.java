package com.repodna.discovery.model;

import java.util.List;

/**
 * Representation of pattern evolution between two graph baselines.
 */
public record PatternEvolution(
    List<Pattern> introduced,
    List<Pattern> strengthened,
    List<Pattern> weakened,
    List<Pattern> disappeared
) {}

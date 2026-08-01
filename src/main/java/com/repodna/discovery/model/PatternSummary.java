package com.repodna.discovery.model;

import java.util.Map;
import java.util.List;

/**
 * High-level summary of all engineering patterns discovered.
 */
public record PatternSummary(
    int totalPatterns,
    double overallConfidence,
    Map<PatternCategory, Integer> patternsByCategory,
    List<PatternGroup> groups
) {}

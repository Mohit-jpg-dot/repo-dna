package com.repodna.dna;

import com.repodna.discovery.model.DiscoveredPattern;
import java.time.Instant;
import java.util.*;

public record DnaProfile(
    String projectName,
    Instant analyzedAt,
    Map<DnaCategory, List<DiscoveredPattern>> patternsByCategory,
    int totalPatterns,
    double overallConfidence,
    List<String> teamSignatures
) {
    /** Get patterns for a specific category */
    public List<DiscoveredPattern> getPatterns(DnaCategory category) {
        return patternsByCategory.getOrDefault(category, List.of());
    }
    
    /** Get the top N most confident patterns */
    public List<DiscoveredPattern> topPatterns(int n) {
        return patternsByCategory.values().stream()
            .flatMap(List::stream)
            .sorted(Comparator.comparingDouble(DiscoveredPattern::confidence).reversed())
            .limit(n)
            .toList();
    }
}

package com.repodna.dna;

import com.repodna.discovery.model.DiscoveredPattern;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DnaEngine {
    /**
     * Build a DNA profile from discovered patterns.
     */
    public DnaProfile buildProfile(String projectName, List<DiscoveredPattern> patterns) {
        // Group patterns by DnaCategory
        Map<DnaCategory, List<DiscoveredPattern>> grouped = patterns.stream()
            .collect(Collectors.groupingBy(p -> DnaCategory.fromPatternCategory(p.category())));
        
        // Sort within each category by confidence descending
        grouped.forEach((k, v) -> v.sort(Comparator.comparingDouble(DiscoveredPattern::confidence).reversed()));
        
        // Calculate overall confidence (weighted average)
        double overallConfidence = patterns.stream()
            .mapToDouble(DiscoveredPattern::confidence)
            .average()
            .orElse(0.0);
        
        // Identify team signatures: top 5 highest-confidence, most distinctive patterns
        List<String> signatures = patterns.stream()
            .filter(p -> p.confidence() >= 0.8)
            .sorted(Comparator.comparingDouble(DiscoveredPattern::confidence).reversed())
            .limit(5)
            .map(DiscoveredPattern::description)
            .toList();
        
        return new DnaProfile(projectName, Instant.now(), grouped, patterns.size(), overallConfidence, signatures);
    }
}

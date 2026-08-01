package com.repodna.discovery.model;

import java.util.List;

public record DiscoveredPattern(
    String id,
    String category,
    String description,
    double confidence,
    int occurrences,
    int totalOpportunities,
    List<PatternEvidence> evidence,
    String reasoning
) {
    /** Convenience: create a high-confidence pattern */
    public static DiscoveredPattern of(String id, String category, String description,
            int occurrences, int total, List<PatternEvidence> evidence, String reasoning) {
        double conf = total > 0 ? (double) occurrences / total : 0.0;
        return new DiscoveredPattern(id, category, description, conf, occurrences, total, evidence, reasoning);
    }
}

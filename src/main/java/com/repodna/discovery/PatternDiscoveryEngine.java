package com.repodna.discovery;

import com.repodna.discovery.detectors.*;
import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrator that runs static analysis detectors over the Repository Knowledge Graph to extract engineering pattern registries.
 */
public class PatternDiscoveryEngine {
    private final RepositoryKnowledgeGraph graph;
    private final List<DiscoveredPattern> discoveredPatterns = new ArrayList<>();

    public PatternDiscoveryEngine(RepositoryKnowledgeGraph graph) {
        this.graph = graph;
    }

    /**
     * Executes all registered pattern detection modules.
     */
    public List<DiscoveredPattern> discover() {
        discoveredPatterns.clear();
        discoveredPatterns.addAll(NamingConventionDetector.detect(graph));
        discoveredPatterns.addAll(SpringPatternDetector.detect(graph));
        discoveredPatterns.addAll(TestingConventionDetector.detect(graph));
        discoveredPatterns.addAll(DependencyBoundaryDetector.detect(graph));
        return getPatterns();
    }

    public List<DiscoveredPattern> getPatterns() {
        return List.copyOf(discoveredPatterns);
    }

    public List<DiscoveredPattern> findPatternsByCategory(PatternCategory category) {
        return discoveredPatterns.stream()
            .filter(p -> p.category() == category)
            .collect(Collectors.toList());
    }

    public List<DiscoveredPattern> findPatternsAboveConfidence(double threshold) {
        return discoveredPatterns.stream()
            .filter(p -> p.confidence() >= threshold)
            .collect(Collectors.toList());
    }
}

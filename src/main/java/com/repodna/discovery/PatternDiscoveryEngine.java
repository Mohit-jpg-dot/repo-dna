package com.repodna.discovery;

import com.repodna.discovery.detectors.*;
import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrator that runs static analysis detectors over the Repository Knowledge Graph to extract engineering pattern registries.
 */
public class PatternDiscoveryEngine {
    private final RepositoryKnowledgeGraph graph;
    private final List<Pattern> cachedPatterns = new ArrayList<>();
    private List<Pattern> cachedCopy = null;
    private boolean isCached = false;

    public PatternDiscoveryEngine(RepositoryKnowledgeGraph graph) {
        this.graph = graph;
    }

    /**
     * Executes all registered pattern detection modules.
     */
    public synchronized List<Pattern> discover() {
        if (isCached) {
            return cachedCopy;
        }

        cachedPatterns.clear();
        cachedPatterns.addAll(NamingConventionDetector.detect(graph));
        cachedPatterns.addAll(SpringPatternDetector.detect(graph));
        cachedPatterns.addAll(TestingConventionDetector.detect(graph));
        cachedPatterns.addAll(DependencyBoundaryDetector.detect(graph));

        cachedCopy = List.copyOf(cachedPatterns);
        isCached = true;
        return cachedCopy;
    }

    /**
     * Clears cached computations to force recalculation.
     */
    public synchronized void invalidateCache() {
        isCached = false;
        cachedCopy = null;
        cachedPatterns.clear();
    }

    public List<Pattern> getPatterns() {
        return discover();
    }

    public List<Pattern> getNamingConventions() {
        return discover().stream()
            .filter(p -> p.category() == PatternCategory.NAMING)
            .collect(Collectors.toList());
    }

    public List<Pattern> getArchitecturalPatterns() {
        return discover().stream()
            .filter(p -> p.category() == PatternCategory.ARCHITECTURE || p.category() == PatternCategory.LAYERING)
            .collect(Collectors.toList());
    }

    public List<Pattern> getDependencyConventions() {
        return discover().stream()
            .filter(p -> p.category() == PatternCategory.DEPENDENCY)
            .collect(Collectors.toList());
    }

    public List<Pattern> getTestingStrategy() {
        return discover().stream()
            .filter(p -> p.category() == PatternCategory.TESTING)
            .collect(Collectors.toList());
    }

    public List<Pattern> getDominantPatterns() {
        return discover().stream()
            .filter(p -> p.confidence().score() >= 0.7)
            .collect(Collectors.toList());
    }

    public List<PatternViolation> getAnomalies() {
        List<PatternViolation> anomalies = new ArrayList<>();
        for (Pattern p : discover()) {
            anomalies.addAll(p.violationsList());
        }
        return anomalies;
    }

    /**
     * Static helper to compare two snapshots and determine pattern evolution.
     */
    public static PatternEvolution compare(RepositoryKnowledgeGraph baseline, RepositoryKnowledgeGraph current) {
        PatternDiscoveryEngine baselineEngine = new PatternDiscoveryEngine(baseline);
        PatternDiscoveryEngine currentEngine = new PatternDiscoveryEngine(current);

        List<Pattern> baselinePatterns = baselineEngine.discover();
        List<Pattern> currentPatterns = currentEngine.discover();

        Map<String, Pattern> baselineMap = new HashMap<>();
        for (Pattern p : baselinePatterns) {
            baselineMap.put(p.id(), p);
        }

        Map<String, Pattern> currentMap = new HashMap<>();
        for (Pattern p : currentPatterns) {
            currentMap.put(p.id(), p);
        }

        List<Pattern> introduced = new ArrayList<>();
        List<Pattern> strengthened = new ArrayList<>();
        List<Pattern> weakened = new ArrayList<>();
        List<Pattern> disappeared = new ArrayList<>();

        for (Pattern cur : currentPatterns) {
            Pattern base = baselineMap.get(cur.id());
            if (base == null) {
                introduced.add(cur);
            } else {
                if (cur.confidence().score() > base.confidence().score()) {
                    strengthened.add(cur);
                } else if (cur.confidence().score() < base.confidence().score()) {
                    weakened.add(cur);
                }
            }
        }

        for (Pattern base : baselinePatterns) {
            if (!currentMap.containsKey(base.id())) {
                disappeared.add(base);
            }
        }

        return new PatternEvolution(introduced, strengthened, weakened, disappeared);
    }
}

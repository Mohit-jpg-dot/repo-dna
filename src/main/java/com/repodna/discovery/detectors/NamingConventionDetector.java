package com.repodna.discovery.detectors;

import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.graph.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministically infers type suffix naming conventions based on class annotations.
 */
public class NamingConventionDetector {

    /**
     * Scans for Controller, Service, and Repository suffixes.
     */
    public static List<DiscoveredPattern> detect(RepositoryKnowledgeGraph graph) {
        List<DiscoveredPattern> patterns = new ArrayList<>();

        detectSuffix(graph, "RestController", "Controller", patterns);
        detectSuffix(graph, "Service", "Service", patterns);
        detectSuffix(graph, "Repository", "Repository", patterns);

        return patterns;
    }

    private static void detectSuffix(RepositoryKnowledgeGraph graph, String annotationName, String expectedSuffix, List<DiscoveredPattern> results) {
        List<RkgNode> nodes = graph.findNodesByAnnotation(annotationName);
        if (nodes.isEmpty()) return;

        int support = 0;
        int total = nodes.size();
        List<PatternEvidence> evidence = new ArrayList<>();
        List<String> exceptions = new ArrayList<>();

        for (RkgNode node : nodes) {
            String className = node.id();
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);

            if (simpleName.endsWith(expectedSuffix)) {
                support++;
                evidence.add(new PatternEvidence(node.id(), "Class ends with " + expectedSuffix, 0, simpleName));
            } else {
                exceptions.add(node.id());
            }
        }

        double confidence = (double) support / total;
        confidence = Math.round(confidence * 100.0) / 100.0;

        results.add(new DiscoveredPattern(
            "naming-suffix-" + expectedSuffix.toLowerCase(),
            PatternCategory.NAMING,
            "Classes annotated with @" + annotationName + " end with '" + expectedSuffix + "'",
            confidence,
            support,
            total,
            "Observed suffix frequency across Spring managed components.",
            evidence,
            exceptions
        ));
    }
}

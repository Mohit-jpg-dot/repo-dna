package com.repodna.discovery.detectors;

import com.repodna.discovery.model.*;
import com.repodna.graph.GraphQueryEngine;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.graph.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Identifies package boundary coupling, allowed dependencies, and layering anomalies.
 */
public class DependencyBoundaryDetector {

    public static List<Pattern> detect(RepositoryKnowledgeGraph graph) {
        List<Pattern> patterns = new ArrayList<>();

        int totalCommits = 1;
        RkgNode gitNode = graph.getNodeById("git-metadata");
        if (gitNode != null && gitNode.metadata().get("commits") != null) {
            totalCommits = ((Number) gitNode.metadata().get("commits")).intValue();
        }

        detectPackageCoupling(graph, totalCommits, patterns);
        detectCircularDependencies(graph, totalCommits, patterns);

        return patterns;
    }

    private static void detectPackageCoupling(RepositoryKnowledgeGraph graph, int totalCommits, List<Pattern> results) {
        Map<String, Map<String, Integer>> packageDeps = new HashMap<>();
        int totalDependencies = 0;

        for (RkgEdge edge : graph.getAllEdges()) {
            if (edge.type() == RkgEdgeType.DEPENDS_ON) {
                RkgNode src = graph.getNodeById(edge.source());
                RkgNode tgt = graph.getNodeById(edge.target());
                if (src != null && tgt != null) {
                    String srcPkg = (String) src.metadata().get("package");
                    String tgtPkg = (String) tgt.metadata().get("package");
                    if (srcPkg != null && tgtPkg != null && !srcPkg.equals(tgtPkg)) {
                        packageDeps.computeIfAbsent(srcPkg, k -> new HashMap<>())
                            .put(tgtPkg, packageDeps.get(srcPkg).getOrDefault(tgtPkg, 0) + 1);
                        totalDependencies++;
                    }
                }
            }
        }

        if (totalDependencies > 0) {
            List<PatternEvidence> evidence = new ArrayList<>();
            List<PatternViolation> violations = new ArrayList<>();

            for (Map.Entry<String, Map<String, Integer>> entry : packageDeps.entrySet()) {
                String src = entry.getKey();
                for (Map.Entry<String, Integer> targetEntry : entry.getValue().entrySet()) {
                    String tgt = targetEntry.getKey();
                    int count = targetEntry.getValue();
                    evidence.add(new PatternEvidence(
                        src,
                        "Package " + src + " depends on " + tgt + " (" + count + " links)",
                        0,
                        src + " -> " + tgt
                    ));
                }
            }

            // High Coupling Anomaly Detection
            for (Map.Entry<String, Map<String, Integer>> entry : packageDeps.entrySet()) {
                if (entry.getValue().size() > 3) {
                    violations.add(new PatternViolation(
                        entry.getKey(),
                        "Package is highly coupled with " + entry.getValue().size() + " other packages",
                        entry.getValue().keySet().toString()
                    ));
                }
            }

            double score = violations.isEmpty() ? 1.0 : 1.0 - ((double) violations.size() / packageDeps.size());
            score = Math.round(score * 100.0) / 100.0;

            results.add(new Pattern(
                "package-coupling-boundaries",
                PatternCategory.DEPENDENCY,
                "Package Dependency Boundaries",
                "Explicit dependency boundaries verified between package namespaces",
                new PatternConfidence(score, packageDeps.size(), violations.size(), 1.0, score),
                new PatternHistory(totalCommits, score, getStabilityByScore(score)),
                evidence,
                violations
            ));
        }
    }

    private static void detectCircularDependencies(RepositoryKnowledgeGraph graph, int totalCommits, List<Pattern> results) {
        GraphQueryEngine queryEngine = new GraphQueryEngine(graph);
        List<String> cycles = queryEngine.findCircularDependencies();

        List<PatternEvidence> evidence = new ArrayList<>();
        List<PatternViolation> violations = new ArrayList<>();

        if (!cycles.isEmpty()) {
            for (String cycle : cycles) {
                violations.add(new PatternViolation("cycle", "Circular dependency cycle detected", cycle));
            }
        } else {
            evidence.add(new PatternEvidence("graph", "No circular dependencies detected", 0, ""));
        }

        double score = cycles.isEmpty() ? 1.0 : 0.0;

        results.add(new Pattern(
            "circular-dependencies",
            PatternCategory.DEPENDENCY,
            "Acyclic Class Dependency Graph",
            "Classes do not participate in circular dependencies",
            new PatternConfidence(score, cycles.isEmpty() ? 1 : 0, cycles.size(), 1.0, score),
            new PatternHistory(totalCommits, score, getStabilityByScore(score)),
            evidence,
            violations
        ));
    }

    private static PatternStability getStabilityByScore(double score) {
        if (score >= 0.9) return PatternStability.STABLE;
        if (score >= 0.7) return PatternStability.EMERGING;
        if (score >= 0.4) return PatternStability.EXPERIMENTAL;
        return PatternStability.DECLINING;
    }
}

package com.repodna.discovery.detectors;

import com.repodna.discovery.model.*;
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

    /**
     * Extracts package-level communication statistics and checks boundary coupling rules.
     */
    public static List<DiscoveredPattern> detect(RepositoryKnowledgeGraph graph) {
        List<DiscoveredPattern> patterns = new ArrayList<>();

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
            List<String> anomalies = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> entry : packageDeps.entrySet()) {
                if (entry.getValue().size() > 3) {
                    anomalies.add(entry.getKey() + " (coupled with " + entry.getValue().keySet() + ")");
                }
            }

            patterns.add(new DiscoveredPattern(
                "package-coupling-boundaries",
                PatternCategory.DEPENDENCY,
                "Explicit dependency boundaries verified between package namespaces",
                0.9,
                packageDeps.size(),
                packageDeps.size(),
                "Observed package dependencies and layer communication coupling.",
                evidence,
                anomalies
            ));
        }

        return patterns;
    }
}

package com.repodna.discovery.detectors;

import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.graph.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifies unit and integration testing conventions in the project's test files.
 */
public class TestingConventionDetector {

    /**
     * Examines test classes for standard test suffix conventions.
     */
    public static List<DiscoveredPattern> detect(RepositoryKnowledgeGraph graph) {
        List<DiscoveredPattern> patterns = new ArrayList<>();

        List<RkgNode> classes = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS) {
                String sourceFile = (String) node.metadata().get("sourceFile");
                if (sourceFile != null && sourceFile.contains("Test")) {
                    classes.add(node);
                }
            }
        }
        if (classes.isEmpty()) return patterns;

        int supportTest = 0;
        int supportTests = 0;
        int supportIT = 0;
        int total = classes.size();

        List<PatternEvidence> testEvidence = new ArrayList<>();
        List<PatternEvidence> testsEvidence = new ArrayList<>();
        List<PatternEvidence> itEvidence = new ArrayList<>();

        for (RkgNode node : classes) {
            String name = node.id();
            int lastDot = name.lastIndexOf('.');
            String simpleName = lastDot == -1 ? name : name.substring(lastDot + 1);

            if (simpleName.endsWith("Test")) {
                supportTest++;
                testEvidence.add(new PatternEvidence(node.id(), "Test ends with Test", 0, simpleName));
            } else if (simpleName.endsWith("Tests")) {
                supportTests++;
                testsEvidence.add(new PatternEvidence(node.id(), "Test ends with Tests", 0, simpleName));
            } else if (simpleName.endsWith("IT")) {
                supportIT++;
                itEvidence.add(new PatternEvidence(node.id(), "Test ends with IT (Integration Test)", 0, simpleName));
            }
        }

        if (total > 0) {
            double testConf = (double) supportTest / total;
            testConf = Math.round(testConf * 100.0) / 100.0;
            patterns.add(new DiscoveredPattern(
                "test-naming-test",
                PatternCategory.TESTING,
                "Test classes end with 'Test' suffix",
                testConf,
                supportTest,
                total,
                "Observed naming frequency of unit tests ending with 'Test'.",
                testEvidence,
                new ArrayList<>()
            ));

            double testsConf = (double) supportTests / total;
            testsConf = Math.round(testsConf * 100.0) / 100.0;
            patterns.add(new DiscoveredPattern(
                "test-naming-tests",
                PatternCategory.TESTING,
                "Test classes end with 'Tests' suffix",
                testsConf,
                supportTests,
                total,
                "Observed naming frequency of test classes ending with 'Tests'.",
                testsEvidence,
                new ArrayList<>()
            ));
        }

        return patterns;
    }
}

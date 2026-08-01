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

    public static List<Pattern> detect(RepositoryKnowledgeGraph graph) {
        List<Pattern> patterns = new ArrayList<>();

        int totalCommits = 1;
        RkgNode gitNode = graph.getNodeById("git-metadata");
        if (gitNode != null && gitNode.metadata().get("commits") != null) {
            totalCommits = ((Number) gitNode.metadata().get("commits")).intValue();
        }

        List<RkgNode> testClasses = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS) {
                String sourceFile = (String) node.metadata().get("sourceFile");
                if (sourceFile != null && (sourceFile.contains("Test") || sourceFile.contains("IT"))) {
                    testClasses.add(node);
                }
            }
        }

        if (testClasses.isEmpty()) return patterns;

        int supportTest = 0;
        int supportTests = 0;
        int supportIT = 0;
        int total = testClasses.size();

        List<PatternEvidence> testEvidence = new ArrayList<>();
        List<PatternEvidence> testsEvidence = new ArrayList<>();
        List<PatternEvidence> itEvidence = new ArrayList<>();
        List<PatternViolation> testViolations = new ArrayList<>();
        List<PatternViolation> testsViolations = new ArrayList<>();

        for (RkgNode node : testClasses) {
            String name = node.id();
            int lastDot = name.lastIndexOf('.');
            String simpleName = lastDot == -1 ? name : name.substring(lastDot + 1);

            if (simpleName.endsWith("Test")) {
                supportTest++;
                testEvidence.add(new PatternEvidence(node.id(), "Test ends with Test", 0, simpleName));
                testsViolations.add(new PatternViolation(node.id(), "Test ends with Test (violates Tests suffix)", simpleName));
            } else if (simpleName.endsWith("Tests")) {
                supportTests++;
                testsEvidence.add(new PatternEvidence(node.id(), "Test ends with Tests", 0, simpleName));
                testViolations.add(new PatternViolation(node.id(), "Test ends with Tests (violates Test suffix)", simpleName));
            } else if (simpleName.endsWith("IT")) {
                supportIT++;
                itEvidence.add(new PatternEvidence(node.id(), "Integration Test ends with IT", 0, simpleName));
            }
        }

        double testScore = (double) supportTest / total;
        testScore = Math.round(testScore * 100.0) / 100.0;
        patterns.add(new Pattern(
            "test-naming-test",
            PatternCategory.TESTING,
            "Test Suffix 'Test'",
            "Unit test classes use the 'Test' suffix",
            new PatternConfidence(testScore, supportTest, testViolations.size(), 1.0, testScore),
            new PatternHistory(totalCommits, testScore, getStabilityByScore(testScore)),
            testEvidence,
            testViolations
        ));

        double testsScore = (double) supportTests / total;
        testsScore = Math.round(testsScore * 100.0) / 100.0;
        patterns.add(new Pattern(
            "test-naming-tests",
            PatternCategory.TESTING,
            "Test Suffix 'Tests'",
            "Unit test classes use the 'Tests' suffix",
            new PatternConfidence(testsScore, supportTests, testsViolations.size(), 1.0, testsScore),
            new PatternHistory(totalCommits, testsScore, getStabilityByScore(testsScore)),
            testsEvidence,
            testsViolations
        ));

        if (supportIT > 0) {
            double itScore = (double) supportIT / total;
            itScore = Math.round(itScore * 100.0) / 100.0;
            patterns.add(new Pattern(
                "test-naming-it",
                PatternCategory.TESTING,
                "Integration Test Suffix 'IT'",
                "Integration test classes use the 'IT' suffix",
                new PatternConfidence(itScore, supportIT, 0, 1.0, itScore),
                new PatternHistory(totalCommits, itScore, getStabilityByScore(itScore)),
                itEvidence,
                new ArrayList<>()
            ));
        }

        return patterns;
    }

    private static PatternStability getStabilityByScore(double score) {
        if (score >= 0.9) return PatternStability.STABLE;
        if (score >= 0.7) return PatternStability.EMERGING;
        if (score >= 0.4) return PatternStability.EXPERIMENTAL;
        return PatternStability.DECLINING;
    }
}

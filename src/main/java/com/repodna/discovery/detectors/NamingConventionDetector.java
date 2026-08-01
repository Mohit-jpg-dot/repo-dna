package com.repodna.discovery.detectors;

import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.graph.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministically infers type suffix naming conventions based on class annotations and types.
 */
public class NamingConventionDetector {

    public static List<Pattern> detect(RepositoryKnowledgeGraph graph) {
        List<Pattern> patterns = new ArrayList<>();

        int totalCommits = 1;
        RkgNode gitNode = graph.getNodeById("git-metadata");
        if (gitNode != null && gitNode.metadata().get("commits") != null) {
            totalCommits = ((Number) gitNode.metadata().get("commits")).intValue();
        }

        detectSuffix(graph, "RestController", "Controller", "naming-suffix-controller", totalCommits, patterns);
        detectSuffix(graph, "Service", "Service", "naming-suffix-service", totalCommits, patterns);
        detectSuffix(graph, "Repository", "Repository", "naming-suffix-repository", totalCommits, patterns);
        detectExceptionNaming(graph, totalCommits, patterns);
        detectDtoNaming(graph, totalCommits, patterns);

        return patterns;
    }

    private static void detectSuffix(RepositoryKnowledgeGraph graph, String annotationName, String expectedSuffix, String patternId, int totalCommits, List<Pattern> results) {
        List<RkgNode> nodes = graph.findNodesByAnnotation(annotationName);
        if (nodes.isEmpty()) return;

        int support = 0;
        int total = nodes.size();
        List<PatternEvidence> evidence = new ArrayList<>();
        List<PatternViolation> violations = new ArrayList<>();

        for (RkgNode node : nodes) {
            String className = node.id();
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);

            if (simpleName.endsWith(expectedSuffix)) {
                support++;
                evidence.add(new PatternEvidence(node.id(), "Class annotated with @" + annotationName + " ends with " + expectedSuffix, 0, simpleName));
            } else {
                violations.add(new PatternViolation(node.id(), "Class annotated with @" + annotationName + " does not end with " + expectedSuffix, simpleName));
            }
        }

        double score = (double) support / total;
        score = Math.round(score * 100.0) / 100.0;

        int totalClasses = graph.findNodesByType(RkgNodeType.CLASS).size();
        double coverage = totalClasses == 0 ? 0.0 : (double) total / totalClasses;
        coverage = Math.round(coverage * 100.0) / 100.0;

        PatternConfidence confidence = new PatternConfidence(score, support, violations.size(), coverage, score);
        PatternStability stability = getStabilityByScore(score);
        PatternHistory history = new PatternHistory(totalCommits, score, stability);

        results.add(new Pattern(
            patternId,
            PatternCategory.NAMING,
            "Naming Suffix: " + expectedSuffix,
            "Classes annotated with @" + annotationName + " should end with '" + expectedSuffix + "'",
            confidence,
            history,
            evidence,
            violations
        ));
    }

    private static void detectExceptionNaming(RepositoryKnowledgeGraph graph, int totalCommits, List<Pattern> results) {
        List<RkgNode> exceptionNodes = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS) {
                // Check extends hierarchy or extends edges
                boolean extendsException = false;
                for (RkgEdge edge : graph.getOutEdges(node.id())) {
                    if (edge.type() == RkgEdgeType.EXTENDS && edge.target().contains("Exception")) {
                        extendsException = true;
                        break;
                    }
                }
                if (extendsException || node.id().endsWith("Exception")) {
                    exceptionNodes.add(node);
                }
            }
        }

        if (exceptionNodes.isEmpty()) return;

        int support = 0;
        int total = exceptionNodes.size();
        List<PatternEvidence> evidence = new ArrayList<>();
        List<PatternViolation> violations = new ArrayList<>();

        for (RkgNode node : exceptionNodes) {
            String className = node.id();
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);

            if (simpleName.endsWith("Exception")) {
                support++;
                evidence.add(new PatternEvidence(node.id(), "Exception class ends with 'Exception'", 0, simpleName));
            } else {
                violations.add(new PatternViolation(node.id(), "Exception class does not end with 'Exception'", simpleName));
            }
        }

        double score = (double) support / total;
        score = Math.round(score * 100.0) / 100.0;

        int totalClasses = graph.findNodesByType(RkgNodeType.CLASS).size();
        double coverage = totalClasses == 0 ? 0.0 : (double) total / totalClasses;
        coverage = Math.round(coverage * 100.0) / 100.0;

        PatternConfidence confidence = new PatternConfidence(score, support, violations.size(), coverage, score);
        PatternStability stability = getStabilityByScore(score);
        PatternHistory history = new PatternHistory(totalCommits, score, stability);

        results.add(new Pattern(
            "naming-exception",
            PatternCategory.NAMING,
            "Exception Naming Suffix",
            "Classes representing exceptions should end with the suffix 'Exception'",
            confidence,
            history,
            evidence,
            violations
        ));
    }

    private static void detectDtoNaming(RepositoryKnowledgeGraph graph, int totalCommits, List<Pattern> results) {
        List<RkgNode> dtoNodes = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS || node.type() == RkgNodeType.RECORD) {
                String pkg = (String) node.metadata().get("package");
                if ((pkg != null && pkg.contains(".dto")) || node.id().endsWith("DTO") || node.id().endsWith("Dto") 
                    || node.id().endsWith("Request") || node.id().endsWith("Response")) {
                    dtoNodes.add(node);
                }
            }
        }

        if (dtoNodes.isEmpty()) return;

        int support = 0;
        int total = dtoNodes.size();
        List<PatternEvidence> evidence = new ArrayList<>();
        List<PatternViolation> violations = new ArrayList<>();

        for (RkgNode node : dtoNodes) {
            String className = node.id();
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);

            if (simpleName.endsWith("DTO") || simpleName.endsWith("Dto") || simpleName.endsWith("Request") || simpleName.endsWith("Response")) {
                support++;
                evidence.add(new PatternEvidence(node.id(), "Data Transfer Object matches naming suffix standard", 0, simpleName));
            } else {
                violations.add(new PatternViolation(node.id(), "Data Transfer Object does not end with expected suffix (DTO/Dto/Request/Response)", simpleName));
            }
        }

        double score = (double) support / total;
        score = Math.round(score * 100.0) / 100.0;

        int totalClasses = graph.findNodesByType(RkgNodeType.CLASS).size();
        double coverage = totalClasses == 0 ? 0.0 : (double) total / totalClasses;
        coverage = Math.round(coverage * 100.0) / 100.0;

        PatternConfidence confidence = new PatternConfidence(score, support, violations.size(), coverage, score);
        PatternStability stability = getStabilityByScore(score);
        PatternHistory history = new PatternHistory(totalCommits, score, stability);

        results.add(new Pattern(
            "naming-dto",
            PatternCategory.NAMING,
            "DTO Naming Convention",
            "Data Transfer Objects should end with 'DTO', 'Dto', 'Request', or 'Response'",
            confidence,
            history,
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

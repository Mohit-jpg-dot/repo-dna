package com.repodna.discovery.detectors;

import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.graph.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects dominant dependency injection and component configuration patterns in the Spring framework.
 */
public class SpringPatternDetector {

    public static List<Pattern> detect(RepositoryKnowledgeGraph graph) {
        List<Pattern> patterns = new ArrayList<>();

        int totalCommits = 1;
        RkgNode gitNode = graph.getNodeById("git-metadata");
        if (gitNode != null && gitNode.metadata().get("commits") != null) {
            totalCommits = ((Number) gitNode.metadata().get("commits")).intValue();
        }

        detectInjectionStyle(graph, totalCommits, patterns);
        detectTransactionPlacement(graph, totalCommits, patterns);

        return patterns;
    }

    private static void detectInjectionStyle(RepositoryKnowledgeGraph graph, int totalCommits, List<Pattern> results) {
        List<RkgNode> components = graph.findNodesByType(RkgNodeType.FRAMEWORK_COMPONENT);
        if (components.isEmpty()) return;

        int constructorCount = 0;
        int fieldCount = 0;
        int total = 0;

        List<PatternEvidence> constructorEvidence = new ArrayList<>();
        List<PatternEvidence> fieldEvidence = new ArrayList<>();
        List<PatternViolation> constructorViolations = new ArrayList<>();
        List<PatternViolation> fieldViolations = new ArrayList<>();

        for (RkgNode component : components) {
            String style = (String) component.metadata().get("injectionStyle");
            if (style == null || "None".equals(style)) continue;

            total++;
            if ("Constructor Injection".equals(style)) {
                constructorCount++;
                constructorEvidence.add(new PatternEvidence(component.id(), "Uses Constructor Injection", 0, ""));
                fieldViolations.add(new PatternViolation(component.id(), "Uses Constructor Injection (violates field-based pattern)", ""));
            } else if ("Field Injection".equals(style)) {
                fieldCount++;
                fieldEvidence.add(new PatternEvidence(component.id(), "Uses Field Injection", 0, ""));
                constructorViolations.add(new PatternViolation(component.id(), "Uses Field Injection (violates constructor-based pattern)", ""));
            }
        }

        if (total > 0) {
            double constructorScore = (double) constructorCount / total;
            constructorScore = Math.round(constructorScore * 100.0) / 100.0;
            double coverage = (double) total / components.size();

            PatternConfidence constructorConf = new PatternConfidence(constructorScore, constructorCount, constructorViolations.size(), coverage, constructorScore);
            PatternStability constructorStability = getStabilityByScore(constructorScore);
            results.add(new Pattern(
                "spring-constructor-injection",
                PatternCategory.SPRING,
                "Spring Constructor Injection",
                "Spring components use constructor-based dependency injection",
                constructorConf,
                new PatternHistory(totalCommits, constructorScore, constructorStability),
                constructorEvidence,
                constructorViolations
            ));

            double fieldScore = (double) fieldCount / total;
            fieldScore = Math.round(fieldScore * 100.0) / 100.0;

            PatternConfidence fieldConf = new PatternConfidence(fieldScore, fieldCount, fieldViolations.size(), coverage, fieldScore);
            PatternStability fieldStability = getStabilityByScore(fieldScore);
            results.add(new Pattern(
                "spring-field-injection",
                PatternCategory.SPRING,
                "Spring Field Injection",
                "Spring components use field-based (@Autowired/@Value) dependency injection",
                fieldConf,
                new PatternHistory(totalCommits, fieldScore, fieldStability),
                fieldEvidence,
                fieldViolations
            ));
        }
    }

    private static void detectTransactionPlacement(RepositoryKnowledgeGraph graph, int totalCommits, List<Pattern> results) {
        int classLevel = 0;
        int methodLevel = 0;
        List<PatternEvidence> evidenceClass = new ArrayList<>();
        List<PatternEvidence> evidenceMethod = new ArrayList<>();

        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS) {
                @SuppressWarnings("unchecked")
                List<String> annos = (List<String>) node.metadata().get("annotations");
                if (annos != null && annos.contains("Transactional")) {
                    classLevel++;
                    evidenceClass.add(new PatternEvidence(node.id(), "Transactional defined at class-level", 0, ""));
                }
            } else if (node.type() == RkgNodeType.METHOD) {
                @SuppressWarnings("unchecked")
                List<String> annos = (List<String>) node.metadata().get("annotations");
                if (annos != null && annos.contains("Transactional")) {
                    methodLevel++;
                    evidenceMethod.add(new PatternEvidence(node.id(), "Transactional defined at method-level", 0, ""));
                }
            }
        }

        int total = classLevel + methodLevel;
        if (total == 0) return;

        double classScore = (double) classLevel / total;
        classScore = Math.round(classScore * 100.0) / 100.0;
        double methodScore = (double) methodLevel / total;
        methodScore = Math.round(methodScore * 100.0) / 100.0;

        PatternConfidence classConf = new PatternConfidence(classScore, classLevel, methodLevel, 1.0, classScore);
        results.add(new Pattern(
            "spring-transactional-class",
            PatternCategory.SPRING,
            "Class-Level Transactions",
            "Transactions are declared at class level using @Transactional",
            classConf,
            new PatternHistory(totalCommits, classScore, getStabilityByScore(classScore)),
            evidenceClass,
            new ArrayList<>()
        ));

        PatternConfidence methodConf = new PatternConfidence(methodScore, methodLevel, classLevel, 1.0, methodScore);
        results.add(new Pattern(
            "spring-transactional-method",
            PatternCategory.SPRING,
            "Method-Level Transactions",
            "Transactions are declared at method level using @Transactional",
            methodConf,
            new PatternHistory(totalCommits, methodScore, getStabilityByScore(methodScore)),
            evidenceMethod,
            new ArrayList<>()
        ));
    }

    private static PatternStability getStabilityByScore(double score) {
        if (score >= 0.9) return PatternStability.STABLE;
        if (score >= 0.7) return PatternStability.EMERGING;
        if (score >= 0.4) return PatternStability.EXPERIMENTAL;
        return PatternStability.DECLINING;
    }
}

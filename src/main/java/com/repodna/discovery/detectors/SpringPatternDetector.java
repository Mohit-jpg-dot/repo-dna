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

    /**
     * Extracts injection style statistics and generates discovered pattern models.
     */
    public static List<DiscoveredPattern> detect(RepositoryKnowledgeGraph graph) {
        List<DiscoveredPattern> patterns = new ArrayList<>();

        List<RkgNode> components = graph.findNodesByType(RkgNodeType.FRAMEWORK_COMPONENT);
        if (components.isEmpty()) return patterns;

        int constructorCount = 0;
        int fieldCount = 0;
        int total = 0;

        List<PatternEvidence> constructorEvidence = new ArrayList<>();
        List<PatternEvidence> fieldEvidence = new ArrayList<>();
        List<String> fieldExceptions = new ArrayList<>();
        List<String> constructorExceptions = new ArrayList<>();

        for (RkgNode component : components) {
            String style = (String) component.metadata().get("injectionStyle");
            if (style == null || "None".equals(style)) continue;

            total++;
            if ("Constructor Injection".equals(style)) {
                constructorCount++;
                constructorEvidence.add(new PatternEvidence(component.id(), "Uses Constructor Injection", 0, ""));
                fieldExceptions.add(component.id());
            } else if ("Field Injection".equals(style)) {
                fieldCount++;
                fieldEvidence.add(new PatternEvidence(component.id(), "Uses Field Injection", 0, ""));
                constructorExceptions.add(component.id());
            }
        }

        if (total > 0) {
            double constructorConfidence = (double) constructorCount / total;
            constructorConfidence = Math.round(constructorConfidence * 100.0) / 100.0;

            patterns.add(new DiscoveredPattern(
                "spring-constructor-injection",
                PatternCategory.SPRING,
                "Spring components use Constructor Injection for dependency resolution",
                constructorConfidence,
                constructorCount,
                total,
                "Observed frequency of constructor injection across all registered beans.",
                constructorEvidence,
                constructorExceptions
            ));

            double fieldConfidence = (double) fieldCount / total;
            fieldConfidence = Math.round(fieldConfidence * 100.0) / 100.0;

            patterns.add(new DiscoveredPattern(
                "spring-field-injection",
                PatternCategory.SPRING,
                "Spring components use Field Injection (@Autowired/@Value) for dependency resolution",
                fieldConfidence,
                fieldCount,
                total,
                "Observed frequency of field-based dependency injection.",
                fieldEvidence,
                fieldExceptions
            ));
        }

        return patterns;
    }
}

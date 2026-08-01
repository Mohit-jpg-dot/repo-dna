package com.repodna.health.evaluators;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.HealthEvaluator;
import com.repodna.health.model.HealthScore;
import com.repodna.graph.LayerDetector;

import java.util.ArrayList;
import java.util.List;

public class ArchitectureEvaluator implements HealthEvaluator {
    @Override
    public String dimension() {
        return "Architecture";
    }

    @Override
    public HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns) {
        int score = 100;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        // Check for layer violations
        if (context.getLayerViolations() != null && !context.getLayerViolations().isEmpty()) {
            int violationDeduction = Math.min(30, context.getLayerViolations().size() * 3);
            score -= violationDeduction;
            improvements.add("Found " + context.getLayerViolations().size() + " layer violations.");
        } else {
            strengths.add("Proper layering with no detected violations.");
        }
        
        // Check missing layers
        if (context.getLayerMap() != null) {
            boolean hasService = context.getLayerMap().containsValue(LayerDetector.Layer.SERVICE);
            boolean hasController = context.getLayerMap().containsValue(LayerDetector.Layer.CONTROLLER);
            if (!hasService) {
                score -= 5;
                improvements.add("Missing standard Service layer.");
            }
            if (!hasController) {
                score -= 5;
                improvements.add("Missing standard Controller layer.");
            }
        }
        
        // Evaluate class graph for coupling/cycles
        if (context.getClassGraph() != null) {
            int maxFanOut = 0;
            for (String v : context.getClassGraph().vertexSet()) {
                int outDegree = context.getClassGraph().outDegreeOf(v);
                if (outDegree > 10) {
                    score -= 2;
                    maxFanOut = Math.max(maxFanOut, outDegree);
                }
            }
            if (maxFanOut > 10) {
                score = Math.max(0, score - 15); // cap deduction
                improvements.add("High coupling detected (classes with fan-out > 10).");
            } else {
                strengths.add("Low coupling among classes.");
            }
        }

        score = Math.max(0, score);
        return new HealthScore(dimension(), score, HealthScore.gradeFor(score), strengths, improvements, List.of());
    }
}

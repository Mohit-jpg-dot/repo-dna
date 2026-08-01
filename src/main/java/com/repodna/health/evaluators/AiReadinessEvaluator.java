package com.repodna.health.evaluators;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.HealthEvaluator;
import com.repodna.health.model.HealthScore;

import java.util.ArrayList;
import java.util.List;

public class AiReadinessEvaluator implements HealthEvaluator {
    @Override
    public String dimension() {
        return "AI Readiness";
    }

    @Override
    public HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns) {
        int score = 30; // base score
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        if (context.getLayerMap() != null && !context.getLayerMap().isEmpty()) {
            score += 15;
            strengths.add("Clear architecture layers detected.");
        } else {
            improvements.add("Unclear or missing architectural layering.");
        }
        
        boolean highConfidenceNaming = patterns.stream()
                .anyMatch(p -> "naming".equalsIgnoreCase(p.category()) && p.confidence() >= 0.8);
        if (highConfidenceNaming) {
            score += 15;
            strengths.add("Consistent naming conventions ease AI context building.");
        } else {
            improvements.add("Inconsistent naming makes AI interpretation harder.");
        }
        
        double avgConfidence = patterns.stream().mapToDouble(DiscoveredPattern::confidence).average().orElse(0.0);
        if (avgConfidence > 0.7) {
            score += 15;
            strengths.add("Highly consistent coding patterns overall.");
        }
        
        if (patterns.stream().anyMatch(p -> "testing".equalsIgnoreCase(p.category()))) {
            score += 10;
            strengths.add("Test coverage provides good AI feedback loop.");
        }
        
        score = Math.max(0, Math.min(100, score));
        return new HealthScore(dimension(), score, HealthScore.gradeFor(score), strengths, improvements, List.of());
    }
}

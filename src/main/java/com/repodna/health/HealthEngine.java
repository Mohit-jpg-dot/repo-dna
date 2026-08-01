package com.repodna.health;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.evaluators.*;
import com.repodna.health.model.*;
import java.util.*;

public class HealthEngine {
    private final List<HealthEvaluator> evaluators;
    
    public HealthEngine() {
        this.evaluators = List.of(
            new ArchitectureEvaluator(),
            new NamingEvaluator(),
            new SecurityEvaluator(),
            new TestingEvaluator(),
            new MaintainabilityEvaluator(),
            new AiReadinessEvaluator()
        );
    }
    
    public HealthReport evaluate(AnalysisContext context, List<DiscoveredPattern> patterns, int rulesLearned) {
        List<HealthScore> scores = new ArrayList<>();
        for (HealthEvaluator evaluator : evaluators) {
            try {
                scores.add(evaluator.evaluate(context, patterns));
            } catch (Exception e) {
                // Create a default score on failure
                scores.add(new HealthScore(evaluator.dimension(), 50, "C", List.of(), 
                    List.of("Evaluation failed: " + e.getMessage()), List.of()));
            }
        }
        
        // Calculate weighted overall score
        int overallScore = (int) scores.stream().mapToInt(HealthScore::score).average().orElse(50);
        
        // AI Readiness is the score from AiReadinessEvaluator
        int aiReadiness = scores.stream()
            .filter(s -> s.dimension().equals("AI Readiness"))
            .mapToInt(HealthScore::score)
            .findFirst().orElse(50);
        
        // Architecture drift = % of layer violations
        double drift = 0.0;
        if (context.getLayerMap() != null && !context.getLayerMap().isEmpty()) {
            int totalLayered = context.getLayerMap().size();
            int violations = context.getLayerViolations() != null ? context.getLayerViolations().size() : 0;
            drift = totalLayered > 0 ? (double) violations / totalLayered * 100 : 0;
        }
        
        int improvements = scores.stream().mapToInt(s -> s.improvements().size()).sum();
        
        return new HealthReport(overallScore, aiReadiness, drift, scores, rulesLearned, improvements);
    }
}

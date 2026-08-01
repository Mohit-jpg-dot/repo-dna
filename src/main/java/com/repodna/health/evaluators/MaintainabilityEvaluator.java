package com.repodna.health.evaluators;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.HealthEvaluator;
import com.repodna.health.model.HealthScore;

import java.util.ArrayList;
import java.util.List;

public class MaintainabilityEvaluator implements HealthEvaluator {
    @Override
    public String dimension() {
        return "Maintainability";
    }

    @Override
    public HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns) {
        int score = 75;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        boolean hasGodClass = patterns.stream().anyMatch(p -> p.description().contains("God Class") || p.description().contains("Large Class"));
        if (hasGodClass) {
            score -= 10;
            improvements.add("God classes detected (large size or high fan-in).");
        } else {
            strengths.add("No massive God classes detected.");
        }
        
        boolean hasConstructorInjection = patterns.stream().anyMatch(p -> p.description().contains("Constructor Injection") && p.confidence() > 0.7);
        if (hasConstructorInjection) {
            score += 10;
            strengths.add("Consistent use of constructor injection.");
        }
        
        score = Math.max(0, Math.min(100, score));
        return new HealthScore(dimension(), score, HealthScore.gradeFor(score), strengths, improvements, List.of());
    }
}

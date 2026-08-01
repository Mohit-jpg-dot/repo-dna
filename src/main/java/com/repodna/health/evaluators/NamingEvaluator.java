package com.repodna.health.evaluators;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.HealthEvaluator;
import com.repodna.health.model.HealthScore;

import java.util.ArrayList;
import java.util.List;

public class NamingEvaluator implements HealthEvaluator {
    @Override
    public String dimension() {
        return "Naming Consistency";
    }

    @Override
    public HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns) {
        int score = 100;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        int namingPatternsCount = 0;
        for (DiscoveredPattern pattern : patterns) {
            if ("naming".equalsIgnoreCase(pattern.category())) {
                namingPatternsCount++;
                if (pattern.confidence() < 0.7) {
                    score -= 5;
                    improvements.add("Inconsistent naming detected: " + pattern.description());
                } else {
                    strengths.add("Consistent naming: " + pattern.description());
                }
            }
        }
        
        if (namingPatternsCount == 0) {
            improvements.add("No distinct naming conventions discovered.");
            score -= 10;
        }

        score = Math.max(0, Math.min(100, score));
        return new HealthScore(dimension(), score, HealthScore.gradeFor(score), strengths, improvements, List.of());
    }
}

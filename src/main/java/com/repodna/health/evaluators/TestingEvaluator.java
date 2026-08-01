package com.repodna.health.evaluators;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.HealthEvaluator;
import com.repodna.health.model.HealthScore;

import java.util.ArrayList;
import java.util.List;

public class TestingEvaluator implements HealthEvaluator {
    @Override
    public String dimension() {
        return "Testing";
    }

    @Override
    public HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns) {
        int score = 70;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        long testFiles = 0;
        long srcFiles = 0;
        
        if (context.getParsedFiles() != null) {
            testFiles = context.getParsedFiles().stream().filter(f -> f.filePath().toString().contains("src/test/")).count();
            srcFiles = context.getParsedFiles().stream().filter(f -> f.filePath().toString().contains("src/main/")).count();
        }
        
        if (srcFiles > 0) {
            double ratio = (double) testFiles / srcFiles;
            if (ratio > 0.8) {
                score += 10;
                strengths.add("Excellent test-to-source ratio (> 0.8).");
            } else if (ratio > 0.5) {
                score += 5;
                strengths.add("Good test-to-source ratio (> 0.5).");
            } else if (ratio < 0.3) {
                score -= 10;
                improvements.add("Poor test-to-source ratio (< 0.3).");
            }
        }
        
        boolean hasMocking = patterns.stream().anyMatch(p -> "testing".equalsIgnoreCase(p.category()) && p.description().contains("Mock"));
        if (hasMocking) {
            score += 5;
            strengths.add("Mock framework usage detected.");
        }
        
        boolean hasIntegration = patterns.stream().anyMatch(p -> "testing".equalsIgnoreCase(p.category()) && p.description().contains("Integration"));
        if (hasIntegration) {
            score += 5;
            strengths.add("Integration tests present.");
        }

        score = Math.max(0, Math.min(100, score));
        return new HealthScore(dimension(), score, HealthScore.gradeFor(score), strengths, improvements, List.of());
    }
}

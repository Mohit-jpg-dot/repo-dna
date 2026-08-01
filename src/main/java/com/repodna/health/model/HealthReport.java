package com.repodna.health.model;

import java.util.List;

public record HealthReport(
    int overallScore,
    int aiReadinessScore,
    double architectureDrift,
    List<HealthScore> dimensionScores,
    int rulesLearned,
    int potentialImprovements
) {
    public String overallGrade() { return HealthScore.gradeFor(overallScore); }
    
    /** Count total improvements across all dimensions */
    public int totalImprovementCount() {
        return dimensionScores.stream()
            .mapToInt(s -> s.improvements().size())
            .sum();
    }
}

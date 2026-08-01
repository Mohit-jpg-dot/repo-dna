package com.repodna.health.model;

import com.repodna.discovery.model.PatternEvidence;
import java.util.List;

public record HealthScore(
    String dimension,
    int score,
    String grade,
    List<String> strengths,
    List<String> improvements,
    List<PatternEvidence> evidence
) {
    public static String gradeFor(int score) {
        if (score >= 90) return "A";
        if (score >= 85) return "B+";
        if (score >= 80) return "B";
        if (score >= 75) return "C+";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}

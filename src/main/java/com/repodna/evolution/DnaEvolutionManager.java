package com.repodna.evolution;

import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.evolution.model.EvolutionScore;

import java.util.*;

/**
 * Manages calculations for the Genetic Distance Architecture Drift (GDAD) algorithm.
 */
public class DnaEvolutionManager {
    private static final double DRIFT_THRESHOLD = 0.15;

    /**
     * Compare baseline DNA patterns with current DNA patterns to evaluate drift and migrations.
     */
    public EvolutionScore calculateDrift(List<DiscoveredPattern> baseline, List<DiscoveredPattern> current) {
        if (baseline.isEmpty() || current.isEmpty()) {
            return new EvolutionScore(0.0, false, List.of(), List.of());
        }

        Map<String, DiscoveredPattern> baseMap = new HashMap<>();
        for (DiscoveredPattern p : baseline) {
            baseMap.put(p.id(), p);
        }

        Map<String, DiscoveredPattern> curMap = new HashMap<>();
        for (DiscoveredPattern p : current) {
            curMap.put(p.id(), p);
        }

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(baseMap.keySet());
        allKeys.addAll(curMap.keySet());

        double weightedDiffSum = 0.0;
        double totalWeight = 0.0;

        List<String> intentionalMigrations = new ArrayList<>();
        List<String> accidentalDrifts = new ArrayList<>();

        for (String key : allKeys) {
            double cBase = baseMap.containsKey(key) ? baseMap.get(key).confidence() : 0.0;
            double cCur = curMap.containsKey(key) ? curMap.get(key).confidence() : 0.0;

            double weight = getPatternWeight(key);
            weightedDiffSum += weight * Math.abs(cBase - cCur);
            totalWeight += weight;

            // Detect drift and migrations
            if (cBase == 0.0 && cCur > 0.0) {
                // New pattern introduced
                if (isKnownRefactoringPattern(key)) {
                    intentionalMigrations.add("Discovered active pattern transition: " + key + " (Confidence: " + Math.round(cCur * 100) + "%)");
                } else {
                    accidentalDrifts.add("Accidental architecture drift detected: anomalous pattern " + key + " introduced.");
                }
            }
        }

        double geneticDistance = totalWeight > 0 ? (weightedDiffSum / totalWeight) : 0.0;
        boolean isDrifted = geneticDistance > DRIFT_THRESHOLD;

        return new EvolutionScore(geneticDistance, isDrifted, intentionalMigrations, accidentalDrifts);
    }

    private double getPatternWeight(String patternId) {
        if (patternId.startsWith("layering") || patternId.startsWith("dependency-boundary")) {
            return 3.0; // High architectural priority
        }
        if (patternId.startsWith("naming") || patternId.startsWith("package-org")) {
            return 1.5; // Medium structural priority
        }
        return 1.0; // Default weight
    }

    private boolean isKnownRefactoringPattern(String patternId) {
        // Known standard migration boundaries (e.g. constructor injection transition)
        return patternId.contains("constructor-injection") || patternId.contains("spring.scheduling");
    }
}

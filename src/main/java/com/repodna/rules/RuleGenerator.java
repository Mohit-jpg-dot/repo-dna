package com.repodna.rules;

import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.rules.model.EngineeringRule;
import java.util.ArrayList;
import java.util.List;

public class RuleGenerator {
    /**
     * Converts discovered patterns into engineering rules if confidence is above the threshold.
     */
    public List<EngineeringRule> generateRules(List<DiscoveredPattern> patterns, double minConfidence) {
        List<EngineeringRule> rules = new ArrayList<>();
        for (DiscoveredPattern pattern : patterns) {
            if (pattern.confidence() >= minConfidence) {
                // Determine a rationale based on the pattern category or description
                String rationale = determineRationale(pattern);
                
                rules.add(new EngineeringRule(
                    pattern.id(),
                    pattern.category(),
                    pattern.description(),
                    rationale,
                    pattern.confidence(),
                    pattern.occurrences(),
                    pattern.evidence(),
                    new ArrayList<>() // Violations populated later by RuleEngine
                ));
            }
        }
        return rules;
    }
    
    private String determineRationale(DiscoveredPattern pattern) {
        if (pattern.reasoning() != null && !pattern.reasoning().isEmpty()) {
            return pattern.reasoning();
        }
        return "Adhering to established conventions improves consistency and makes it easier for AI coding agents to write compatible code.";
    }
}

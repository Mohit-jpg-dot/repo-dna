package com.repodna.health.evaluators;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.health.HealthEvaluator;
import com.repodna.health.model.HealthScore;

import java.util.ArrayList;
import java.util.List;

public class SecurityEvaluator implements HealthEvaluator {
    @Override
    public String dimension() {
        return "Security";
    }

    @Override
    public HealthScore evaluate(AnalysisContext context, List<DiscoveredPattern> patterns) {
        int score = 70;
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        
        boolean hasSecurityConfig = false;
        boolean hasMethodSecurity = false;
        
        for (DiscoveredPattern pattern : patterns) {
            if ("security".equalsIgnoreCase(pattern.category())) {
                if (pattern.description().contains("config") || pattern.description().contains("SecurityFilterChain")) {
                    hasSecurityConfig = true;
                }
                if (pattern.description().contains("PreAuthorize") || pattern.description().contains("Secured")) {
                    hasMethodSecurity = true;
                }
            }
        }
        
        if (hasSecurityConfig) {
            score += 10;
            strengths.add("Security configuration class present.");
        } else {
            score -= 20;
            improvements.add("No clear security configuration detected.");
        }
        
        if (hasMethodSecurity) {
            score += 10;
            strengths.add("Method-level security is in use.");
        }
        
        boolean fieldInjection = patterns.stream()
                .anyMatch(p -> p.description().contains("Autowired") && p.description().toLowerCase().contains("field"));
        if (fieldInjection) {
            score -= 5;
            improvements.add("Field injection detected, increasing attack surface/mutability.");
        }
        
        score = Math.max(0, Math.min(100, score));
        return new HealthScore(dimension(), score, HealthScore.gradeFor(score), strengths, improvements, List.of());
    }
}

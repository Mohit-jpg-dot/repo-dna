package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.graph.LayerDetector;

import java.util.ArrayList;
import java.util.List;

public class LayeringPatternDetector implements PatternDetector {
    @Override
    public String name() {
        return "Layering Pattern Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        
        List<LayerDetector.LayerViolation> violations = context.getLayerViolations();
        int totalClasses = context.getLayerMap().size();
        
        if (totalClasses > 0) {
            int violatingClasses = (int) violations.stream().map(LayerDetector.LayerViolation::sourceClass).distinct().count();
            int compliantClasses = totalClasses - violatingClasses;
            
            List<PatternEvidence> evidence = new ArrayList<>();
            for (LayerDetector.LayerViolation v : violations) {
                evidence.add(new PatternEvidence(null, 0, 
                        v.sourceClass() + " -> " + v.targetClass(), 
                        "Violation: " + v.sourceLayer() + " to " + v.targetLayer()));
            }
            
            patterns.add(DiscoveredPattern.of(
                "architecture.layered", "architecture", "Layered Architecture Compliance",
                compliantClasses, totalClasses, evidence,
                "Checks if dependencies flow correctly (e.g., Controller -> Service -> Repository)."
            ));
        }
        
        return patterns;
    }
}

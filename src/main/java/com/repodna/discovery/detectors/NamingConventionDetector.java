package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.graph.LayerDetector;

import java.util.ArrayList;
import java.util.List;

public class NamingConventionDetector implements PatternDetector {
    @Override
    public String name() {
        return "Naming Convention Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        
        // Controllers
        List<String> controllers = context.getClassesInLayer(LayerDetector.Layer.CONTROLLER);
        patterns.add(checkSuffix(controllers, "Controller", "naming.controller-suffix", "Controller naming convention"));
        
        // Services
        List<String> services = context.getClassesInLayer(LayerDetector.Layer.SERVICE);
        patterns.add(checkSuffix(services, "Service", "naming.service-suffix", "Service naming convention", "ServiceImpl"));
        
        // Repositories
        List<String> repositories = context.getClassesInLayer(LayerDetector.Layer.REPOSITORY);
        patterns.add(checkSuffix(repositories, "Repository", "naming.repository-suffix", "Repository naming convention"));
        
        return patterns.stream().filter(p -> p != null && p.totalOpportunities() > 0).toList();
    }
    
    private DiscoveredPattern checkSuffix(List<String> classes, String suffix, String id, String desc, String... alternativeSuffixes) {
        if (classes.isEmpty()) return null;
        
        int occurrences = 0;
        List<PatternEvidence> evidence = new ArrayList<>();
        
        for (String cls : classes) {
            String shortName = cls.substring(cls.lastIndexOf('.') + 1);
            boolean matches = shortName.endsWith(suffix);
            if (!matches && alternativeSuffixes != null) {
                for (String alt : alternativeSuffixes) {
                    if (shortName.endsWith(alt)) {
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) {
                occurrences++;
                evidence.add(new PatternEvidence(null, 0, shortName, "Matches suffix convention"));
            } else {
                evidence.add(new PatternEvidence(null, 0, shortName, "Does not match expected suffix " + suffix));
            }
        }
        
        return DiscoveredPattern.of(
            id, "naming", desc, occurrences, classes.size(), evidence,
            "Consistent naming helps in identifying the role of a class."
        );
    }
}

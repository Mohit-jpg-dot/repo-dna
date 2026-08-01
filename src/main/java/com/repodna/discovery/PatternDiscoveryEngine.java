package com.repodna.discovery;

import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.detectors.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class PatternDiscoveryEngine {
    private final List<PatternDetector> detectors;
    
    public PatternDiscoveryEngine() {
        this.detectors = new ArrayList<>();
        ServiceLoader.load(PatternDetector.class).forEach(this.detectors::add);
        if (this.detectors.isEmpty()) {
            this.detectors.addAll(List.of(
                new NamingConventionDetector(),
                new PackageOrganizationDetector(),
                new LayeringPatternDetector(),
                new ExceptionHandlingDetector(),
                new LoggingPatternDetector(),
                new CachingStrategyDetector(),
                new DependencyBoundaryDetector(),
                new DtoStrategyDetector(),
                new RestConventionDetector(),
                new SecurityPatternDetector(),
                new SpringPatternDetector(),
                new TestingConventionDetector(),
                new TransactionPatternDetector(),
                new ValidationPatternDetector()
            ));
        }
    }
    
    public List<DiscoveredPattern> discoverPatterns(AnalysisContext context) {
        List<DiscoveredPattern> allPatterns = new ArrayList<>();
        for (PatternDetector detector : detectors) {
            try {
                List<DiscoveredPattern> patterns = detector.detect(context);
                allPatterns.addAll(patterns);
            } catch (Exception e) {
                System.err.println("Warning: detector '" + detector.name() + "' failed: " + e.getMessage());
            }
        }
        // Filter low confidence patterns
        return allPatterns.stream()
            .filter(p -> p.confidence() >= 0.3)
            .sorted(Comparator.comparingDouble(DiscoveredPattern::confidence).reversed())
            .toList();
    }
    
    public int detectorCount() { return detectors.size(); }
}

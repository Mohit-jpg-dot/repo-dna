package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.parser.model.AnnotationDecl;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.FieldDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects Spring-specific patterns in the application.
 */
public class SpringPatternDetector implements PatternDetector {
    @Override
    public String name() {
        return "Spring Pattern Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        
        List<PatternEvidence> profileEvidence = new ArrayList<>();
        List<PatternEvidence> configPropsEvidence = new ArrayList<>();
        List<PatternEvidence> valueInjEvidence = new ArrayList<>();
        List<PatternEvidence> eventEvidence = new ArrayList<>();
        List<PatternEvidence> asyncEvidence = new ArrayList<>();
        
        int profileCount = 0;
        int configPropsCount = 0;
        int valueInjCount = 0;
        int eventCount = 0;
        int asyncCount = 0;
        
        Map<String, Integer> componentDistribution = new HashMap<>();

        for (ClassDecl clazz : context.allClasses()) {
            Path filePath = getFilePath(context, clazz);
            for (AnnotationDecl ann : clazz.annotations()) {
                if (ann.name().endsWith("Profile")) {
                    profileCount++;
                    profileEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "@Profile", "Profile usage"));
                } else if (ann.name().endsWith("ConfigurationProperties")) {
                    configPropsCount++;
                    configPropsEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "@ConfigurationProperties", "Type-safe config binding"));
                } else if (ann.name().endsWith("Component") || ann.name().endsWith("Service") || 
                           ann.name().endsWith("Repository") || ann.name().endsWith("Controller") || 
                           ann.name().endsWith("RestController") || ann.name().endsWith("Configuration")) {
                    String baseName = ann.name().substring(ann.name().lastIndexOf('.') + 1);
                    componentDistribution.put(baseName, componentDistribution.getOrDefault(baseName, 0) + 1);
                } else if (ann.name().endsWith("Async")) {
                    asyncCount++;
                    asyncEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "@Async", "Class-level Async processing"));
                }
            }

            for (FieldDecl field : clazz.fields()) {
                for (AnnotationDecl ann : field.annotations()) {
                    if (ann.name().endsWith("Value")) {
                        valueInjCount++;
                        valueInjEvidence.add(new PatternEvidence(filePath, field.startLine(), "@Value on " + field.name(), "Value injection"));
                    }
                }
            }

            for (MethodDecl method : clazz.methods()) {
                for (AnnotationDecl ann : method.annotations()) {
                    if (ann.name().endsWith("EventListener")) {
                        eventCount++;
                        eventEvidence.add(new PatternEvidence(filePath, method.startLine(), "@EventListener on " + method.name(), "Event driven pattern"));
                    } else if (ann.name().endsWith("Async")) {
                        asyncCount++;
                        asyncEvidence.add(new PatternEvidence(filePath, method.startLine(), "@Async on " + method.name(), "Method-level Async processing"));
                    }
                }
            }
        }

        if (profileCount > 0) {
            patterns.add(DiscoveredPattern.of("spring.profile-usage", "Spring Patterns", "Usage of @Profile for environment-specific beans", profileCount, context.allClasses().size(), profileEvidence, "Found " + profileCount + " usages of @Profile"));
        }
        if (configPropsCount > 0) {
            patterns.add(DiscoveredPattern.of("spring.config-properties", "Spring Patterns", "Usage of @ConfigurationProperties", configPropsCount, context.allClasses().size(), configPropsEvidence, "Found " + configPropsCount + " usages of @ConfigurationProperties"));
        }
        if (valueInjCount > 0) {
            patterns.add(DiscoveredPattern.of("spring.value-injection", "Spring Patterns", "Usage of @Value for property injection", valueInjCount, context.allClasses().size(), valueInjEvidence, "Found " + valueInjCount + " usages of @Value"));
        }
        if (eventCount > 0) {
            patterns.add(DiscoveredPattern.of("spring.event-driven", "Spring Patterns", "Usage of Spring events (@EventListener)", eventCount, context.allMethods().size(), eventEvidence, "Found " + eventCount + " usages of @EventListener"));
        }
        if (asyncCount > 0) {
            patterns.add(DiscoveredPattern.of("spring.async-processing", "Spring Patterns", "Usage of @Async for asynchronous processing", asyncCount, context.allMethods().size(), asyncEvidence, "Found " + asyncCount + " usages of @Async"));
        }
        if (!componentDistribution.isEmpty()) {
            List<PatternEvidence> distributionEvidence = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : componentDistribution.entrySet()) {
                distributionEvidence.add(new PatternEvidence(Path.of("global"), 1, entry.getKey(), entry.getValue() + " instances"));
            }
            int totalComponents = componentDistribution.values().stream().mapToInt(Integer::intValue).sum();
            patterns.add(DiscoveredPattern.of("spring.component-distribution", "Spring Patterns", "Distribution of Spring stereotype annotations", totalComponents, totalComponents, distributionEvidence, "Component distribution: " + componentDistribution));
        }

        return patterns;
    }

    private Path getFilePath(AnalysisContext context, ClassDecl clazz) {
        if (context.getParsedFiles() != null) {
            for (ParsedFile pf : context.getParsedFiles()) {
                if (pf.classes() != null && pf.classes().contains(clazz)) {
                    return pf.filePath();
                }
            }
        }
        return Path.of("unknown");
    }
}

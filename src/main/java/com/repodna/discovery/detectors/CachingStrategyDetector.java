package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.PatternDetector;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.discovery.model.PatternEvidence;
import com.repodna.graph.LayerDetector.Layer;
import com.repodna.parser.model.AnnotationDecl;
import com.repodna.parser.model.ClassDecl;
import com.repodna.parser.model.MethodDecl;
import com.repodna.parser.model.ParsedFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects caching patterns and strategies in the application.
 */
public class CachingStrategyDetector implements PatternDetector {
    @Override
    public String name() {
        return "Caching Strategy Detector";
    }

    @Override
    public List<DiscoveredPattern> detect(AnalysisContext context) {
        List<DiscoveredPattern> patterns = new ArrayList<>();
        
        List<PatternEvidence> enableCachingEvidence = new ArrayList<>();
        List<PatternEvidence> serviceLevelEvidence = new ArrayList<>();
        List<PatternEvidence> evictionEvidence = new ArrayList<>();
        List<PatternEvidence> cacheNamesEvidence = new ArrayList<>();
        
        int enableCachingCount = 0;
        int serviceCacheCount = 0;
        int evictionCount = 0;
        Set<String> cacheNames = new HashSet<>();

        for (ClassDecl clazz : context.allClasses()) {
            Path filePath = getFilePath(context, clazz);
            for (AnnotationDecl ann : clazz.annotations()) {
                if (ann.name().endsWith("EnableCaching")) {
                    enableCachingCount++;
                    enableCachingEvidence.add(new PatternEvidence(filePath, clazz.startLine(), "@EnableCaching", "Caching enabled in configuration"));
                }
            }

            boolean isService = context.getClassesInLayer(Layer.SERVICE).contains(clazz.name());

            for (MethodDecl method : clazz.methods()) {
                for (AnnotationDecl ann : method.annotations()) {
                    if (ann.name().endsWith("Cacheable") || ann.name().endsWith("CachePut")) {
                        if (isService) {
                            serviceCacheCount++;
                            serviceLevelEvidence.add(new PatternEvidence(filePath, method.startLine(), "@" + ann.name() + " on " + method.name(), "Caching applied at service layer"));
                        }
                        
                        if (ann.attributes() != null) {
                            String value = ann.attributes().get("value");
                            String cacheNamesVal = ann.attributes().get("cacheNames");
                            String nameToUse = value != null ? value : cacheNamesVal;
                            if (nameToUse != null) {
                                cacheNames.add(nameToUse);
                                cacheNamesEvidence.add(new PatternEvidence(filePath, method.startLine(), nameToUse, "Cache name used"));
                            }
                        }
                    } else if (ann.name().endsWith("CacheEvict")) {
                        evictionCount++;
                        evictionEvidence.add(new PatternEvidence(filePath, method.startLine(), "@CacheEvict on " + method.name(), "Cache eviction pattern"));
                    }
                }
            }
        }

        if (enableCachingCount > 0) {
            patterns.add(DiscoveredPattern.of("caching.enabled", "Caching Strategy", "Application enables caching via @EnableCaching", enableCachingCount, enableCachingCount, enableCachingEvidence, "Found @EnableCaching"));
        }
        if (serviceCacheCount > 0) {
            patterns.add(DiscoveredPattern.of("caching.service-level", "Caching Strategy", "Caching is applied at the service layer", serviceCacheCount, context.allMethods().size(), serviceLevelEvidence, "Found " + serviceCacheCount + " service methods with caching annotations"));
        }
        if (evictionCount > 0) {
            patterns.add(DiscoveredPattern.of("caching.eviction-pattern", "Caching Strategy", "Usage of @CacheEvict to manage cache staleness", evictionCount, context.allMethods().size(), evictionEvidence, "Found " + evictionCount + " usages of @CacheEvict"));
        }
        if (!cacheNames.isEmpty()) {
            patterns.add(DiscoveredPattern.of("caching.cache-names", "Caching Strategy", "Distinct cache names identified", cacheNames.size(), cacheNames.size(), cacheNamesEvidence, "Identified " + cacheNames.size() + " distinct cache name declarations"));
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

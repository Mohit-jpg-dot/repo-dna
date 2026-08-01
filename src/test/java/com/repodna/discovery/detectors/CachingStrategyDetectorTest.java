package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.parser.model.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CachingStrategyDetectorTest {

    @Test
    public void testDetectNoCaching() {
        CachingStrategyDetector detector = new CachingStrategyDetector();
        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(), null, null, Map.of(), List.of()
        );
        List<DiscoveredPattern> patterns = detector.detect(context);
        assertTrue(patterns.isEmpty());
    }

    @Test
    public void testDetectCachingAnnotations() {
        CachingStrategyDetector detector = new CachingStrategyDetector();

        // Method with Cacheable annotation
        Map<String, String> attrs = new HashMap<>();
        attrs.put("value", "\"users\"");
        AnnotationDecl cacheableAnn = new AnnotationDecl("Cacheable", attrs);
        MethodDecl method = new MethodDecl("getUser", "User", List.of(), Set.of("public"), List.of(cacheableAnn), 4, List.of(), false, 15);

        // Class with EnableCaching annotation
        AnnotationDecl enableAnn = new AnnotationDecl("EnableCaching", Map.of());
        ClassDecl clazz = new ClassDecl("CacheConfig", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(enableAnn), List.of(method), List.of(), List.of(), 1);

        ParsedFile pf = new ParsedFile(
            Path.of("src/main/java/com/example/CacheConfig.java"),
            "com.example",
            List.of(),
            List.of(clazz)
        );

        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(pf), null, null, Map.of(), List.of()
        );

        List<DiscoveredPattern> patterns = detector.detect(context);
        assertFalse(patterns.isEmpty());

        // Check for caching enabled pattern
        boolean hasEnabled = patterns.stream().anyMatch(p -> p.id().equals("caching.enabled"));
        assertTrue(hasEnabled);

        // Check for cache names pattern
        boolean hasCacheNames = patterns.stream().anyMatch(p -> p.id().equals("caching.cache-names"));
        assertTrue(hasCacheNames);
    }
}

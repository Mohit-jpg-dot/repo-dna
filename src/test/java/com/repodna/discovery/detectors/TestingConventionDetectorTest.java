package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.parser.model.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestingConventionDetectorTest {

    @Test
    public void testDetectNoFiles() {
        TestingConventionDetector detector = new TestingConventionDetector();
        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(), null, null, Map.of(), List.of()
        );
        List<DiscoveredPattern> patterns = detector.detect(context);
        assertTrue(patterns.isEmpty());
    }

    @Test
    public void testDetectMockitoAndAssertJ() {
        TestingConventionDetector detector = new TestingConventionDetector();

        // Mock test file
        ImportDecl mockitoImp = new ImportDecl("org.mockito.Mock", false, false);
        ImportDecl assertjImp = new ImportDecl("org.assertj.core.api.Assertions", false, false);
        
        AnnotationDecl testAnn = new AnnotationDecl("Test", Map.of());
        MethodDecl method = new MethodDecl("shouldTestSomething", "void", List.of(), Set.of(), List.of(testAnn), 5, List.of(), false, 10);
        
        ClassDecl clazz = new ClassDecl("MyServiceTest", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(), List.of(method), List.of(), List.of(), 1);
        
        ParsedFile pf = new ParsedFile(
            Path.of("src/test/java/com/example/MyServiceTest.java"),
            "com.example",
            List.of(mockitoImp, assertjImp),
            List.of(clazz)
        );

        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(pf), null, null, Map.of(), List.of()
        );

        List<DiscoveredPattern> patterns = detector.detect(context);
        assertFalse(patterns.isEmpty());
        
        // Assert mock framework detected
        boolean hasMock = patterns.stream().anyMatch(p -> p.id().equals("testing.mock-framework"));
        assertTrue(hasMock);
        
        // Assert assertion library detected
        boolean hasAssert = patterns.stream().anyMatch(p -> p.id().equals("testing.assertion-library"));
        assertTrue(hasAssert);
        
        // Assert naming style detected
        boolean hasNaming = patterns.stream().anyMatch(p -> p.id().equals("testing.naming-style"));
        assertTrue(hasNaming);
    }
}

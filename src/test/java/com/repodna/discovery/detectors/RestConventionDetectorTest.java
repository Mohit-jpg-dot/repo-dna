package com.repodna.discovery.detectors;

import com.repodna.discovery.AnalysisContext;
import com.repodna.discovery.model.DiscoveredPattern;
import com.repodna.graph.LayerDetector.Layer;
import com.repodna.parser.model.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class RestConventionDetectorTest {

    @Test
    public void testDetectNoControllers() {
        RestConventionDetector detector = new RestConventionDetector();
        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(), null, null, Map.of(), List.of()
        );
        List<DiscoveredPattern> patterns = detector.detect(context);
        assertTrue(patterns.isEmpty());
    }

    @Test
    public void testDetectRestEndpoints() {
        RestConventionDetector detector = new RestConventionDetector();

        // Mock controller method and class
        AnnotationDecl getAnn = new AnnotationDecl("GetMapping", Map.of("value", "\"/users\""));
        MethodDecl method = new MethodDecl("getUsers", "ResponseEntity<List<User>>", List.of(), Set.of("public"), List.of(getAnn), 3, List.of(), false, 12);
        
        ClassDecl clazz = new ClassDecl("UserController", ClassDecl.ClassType.CLASS, Set.of("public"), null, List.of(), List.of(), List.of(method), List.of(), List.of(), 1);
        
        ParsedFile pf = new ParsedFile(
            Path.of("src/main/java/com/example/UserController.java"),
            "com.example",
            List.of(),
            List.of(clazz)
        );

        // Map clazz FQN to Controller layer
        Map<String, Layer> layerMap = Map.of("com.example.UserController", Layer.CONTROLLER);

        AnalysisContext context = new AnalysisContext(
            null, List.of(), List.of(pf), null, null, layerMap, List.of()
        );

        List<DiscoveredPattern> patterns = detector.detect(context);
        assertFalse(patterns.isEmpty());
        
        // Assert http methods patterns found
        boolean hasHttp = patterns.stream().anyMatch(p -> p.id().equals("rest.http-methods"));
        assertTrue(hasHttp);

        // Assert response wrapping found (uses ResponseEntity)
        boolean hasWrapping = patterns.stream().anyMatch(p -> p.id().equals("rest.response-wrapping"));
        assertTrue(hasWrapping);
    }
}

package com.repodna.discovery;

import com.repodna.discovery.model.*;
import com.repodna.graph.RepositoryKnowledgeGraph;
import com.repodna.graph.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class PatternDiscoveryEngineTest {

    @Test
    public void shouldDiscoverNamingConventions() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        // 1 Service ending with Service (conformant)
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("annotations", List.of("Service"));
        RkgNode conformant = new RkgNode("com.example.UserService", RkgNodeType.CLASS, "UserService.java", meta1, 1);

        // 1 Service not ending with Service (exception)
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("annotations", List.of("Service"));
        RkgNode exception = new RkgNode("com.example.UserProcessor", RkgNodeType.CLASS, "UserProcessor.java", meta2, 1);

        graph.addNode(conformant);
        graph.addNode(exception);

        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);
        List<DiscoveredPattern> patterns = engine.discover();

        DiscoveredPattern namingPattern = patterns.stream()
            .filter(p -> "naming-suffix-service".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(namingPattern.confidence()).isEqualTo(0.5);
        assertThat(namingPattern.occurrences()).isEqualTo(1);
        assertThat(namingPattern.totalOpportunities()).isEqualTo(2);
        assertThat(namingPattern.exceptionsList()).containsExactly("com.example.UserProcessor");
    }

    @Test
    public void shouldDiscoverSpringInjectionPatterns() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        // Bean 1: Constructor Injection
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("injectionStyle", "Constructor Injection");
        RkgNode bean1 = new RkgNode("com.example.UserService$SpringBean", RkgNodeType.FRAMEWORK_COMPONENT, "UserService.java", meta1, 1);

        // Bean 2: Field Injection
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("injectionStyle", "Field Injection");
        RkgNode bean2 = new RkgNode("com.example.OrderService$SpringBean", RkgNodeType.FRAMEWORK_COMPONENT, "OrderService.java", meta2, 1);

        graph.addNode(bean1);
        graph.addNode(bean2);

        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);
        List<DiscoveredPattern> patterns = engine.discover();

        DiscoveredPattern constructorPattern = patterns.stream()
            .filter(p -> "spring-constructor-injection".equals(p.id()))
            .findFirst()
            .orElseThrow();

        DiscoveredPattern fieldPattern = patterns.stream()
            .filter(p -> "spring-field-injection".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(constructorPattern.confidence()).isEqualTo(0.5);
        assertThat(constructorPattern.occurrences()).isEqualTo(1);
        assertThat(constructorPattern.totalOpportunities()).isEqualTo(2);
        assertThat(constructorPattern.exceptionsList()).containsExactly("com.example.OrderService$SpringBean");

        assertThat(fieldPattern.confidence()).isEqualTo(0.5);
        assertThat(fieldPattern.exceptionsList()).containsExactly("com.example.UserService$SpringBean");
    }

    @Test
    public void shouldDiscoverTestingConventions() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("sourceFile", "src/test/java/com/example/UserServiceTest.java");
        RkgNode test1 = new RkgNode("com.example.UserServiceTest", RkgNodeType.CLASS, "UserServiceTest.java", meta1, 1);

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("sourceFile", "src/test/java/com/example/UserControllerTests.java");
        RkgNode test2 = new RkgNode("com.example.UserControllerTests", RkgNodeType.CLASS, "UserControllerTests.java", meta2, 1);

        graph.addNode(test1);
        graph.addNode(test2);

        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);
        List<DiscoveredPattern> patterns = engine.discover();

        DiscoveredPattern suffixTest = patterns.stream()
            .filter(p -> "test-naming-test".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(suffixTest.occurrences()).isEqualTo(1);
        assertThat(suffixTest.totalOpportunities()).isEqualTo(2);
        assertThat(suffixTest.confidence()).isEqualTo(0.5);
    }

    @Test
    public void shouldDiscoverDependencyBoundariesAndCoupling() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("package", "com.example.controller");
        RkgNode controller = new RkgNode("com.example.controller.UserController", RkgNodeType.CLASS, "UserController.java", meta1, 1);

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("package", "com.example.service");
        RkgNode service = new RkgNode("com.example.service.UserService", RkgNodeType.CLASS, "UserService.java", meta2, 1);

        graph.addNode(controller);
        graph.addNode(service);
        graph.addEdge(new RkgEdge("com.example.controller.UserController", "com.example.service.UserService", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));

        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);
        List<DiscoveredPattern> patterns = engine.discover();

        DiscoveredPattern boundaryPattern = patterns.stream()
            .filter(p -> "package-coupling-boundaries".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(boundaryPattern.occurrences()).isEqualTo(1);
        assertThat(boundaryPattern.evidenceList().get(0).snippet()).isEqualTo("com.example.controller -> com.example.service");
    }
}

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

        // 1 Service not ending with Service (exception/violation)
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("annotations", List.of("Service"));
        RkgNode violation = new RkgNode("com.example.UserProcessor", RkgNodeType.CLASS, "UserProcessor.java", meta2, 1);

        graph.addNode(conformant);
        graph.addNode(violation);

        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);
        List<Pattern> patterns = engine.discover();

        Pattern namingPattern = patterns.stream()
            .filter(p -> "naming-suffix-service".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(namingPattern.confidence().score()).isEqualTo(0.5);
        assertThat(namingPattern.confidence().supportCount()).isEqualTo(1);
        assertThat(namingPattern.confidence().violationCount()).isEqualTo(1);
        assertThat(namingPattern.violationsList().get(0).nodeId()).isEqualTo("com.example.UserProcessor");
        assertThat(namingPattern.history().stability()).isEqualTo(PatternStability.EXPERIMENTAL);
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
        List<Pattern> patterns = engine.discover();

        Pattern constructorPattern = patterns.stream()
            .filter(p -> "spring-constructor-injection".equals(p.id()))
            .findFirst()
            .orElseThrow();

        Pattern fieldPattern = patterns.stream()
            .filter(p -> "spring-field-injection".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(constructorPattern.confidence().score()).isEqualTo(0.5);
        assertThat(constructorPattern.confidence().supportCount()).isEqualTo(1);
        assertThat(constructorPattern.violationsList().get(0).nodeId()).isEqualTo("com.example.OrderService$SpringBean");

        assertThat(fieldPattern.confidence().score()).isEqualTo(0.5);
        assertThat(fieldPattern.violationsList().get(0).nodeId()).isEqualTo("com.example.UserService$SpringBean");
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
        List<Pattern> patterns = engine.discover();

        Pattern suffixTest = patterns.stream()
            .filter(p -> "test-naming-test".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(suffixTest.confidence().supportCount()).isEqualTo(1);
        assertThat(suffixTest.confidence().violationCount()).isEqualTo(1);
        assertThat(suffixTest.confidence().score()).isEqualTo(0.5);
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
        List<Pattern> patterns = engine.discover();

        Pattern boundaryPattern = patterns.stream()
            .filter(p -> "package-coupling-boundaries".equals(p.id()))
            .findFirst()
            .orElseThrow();

        assertThat(boundaryPattern.confidence().supportCount()).isEqualTo(1);
        assertThat(boundaryPattern.evidenceList().get(0).snippet()).isEqualTo("com.example.controller -> com.example.service");
    }

    @Test
    public void shouldCacheDiscoveredPatterns() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();
        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);

        List<Pattern> run1 = engine.discover();
        List<Pattern> run2 = engine.discover();

        // Check reference identity to confirm caching
        assertThat(run1).isSameAs(run2);

        engine.invalidateCache();
        List<Pattern> run3 = engine.discover();
        assertThat(run1).isNotSameAs(run3);
    }

    @Test
    public void shouldCompareSnapshotsForEvolution() {
        RepositoryKnowledgeGraph baseline = new RepositoryKnowledgeGraph();
        RepositoryKnowledgeGraph current = new RepositoryKnowledgeGraph();

        // Baseline has constructor injection for 1 bean (confidence 1.0)
        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("injectionStyle", "Constructor Injection");
        RkgNode beanBase = new RkgNode("com.example.UserService$SpringBean", RkgNodeType.FRAMEWORK_COMPONENT, "UserService.java", meta1, 1);
        baseline.addNode(beanBase);

        // Current has constructor injection for 1 bean and field injection for 1 bean (confidence drops to 0.5)
        RkgNode beanCur1 = new RkgNode("com.example.UserService$SpringBean", RkgNodeType.FRAMEWORK_COMPONENT, "UserService.java", meta1, 1);
        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("injectionStyle", "Field Injection");
        RkgNode beanCur2 = new RkgNode("com.example.OrderService$SpringBean", RkgNodeType.FRAMEWORK_COMPONENT, "OrderService.java", meta2, 1);
        current.addNode(beanCur1);
        current.addNode(beanCur2);

        PatternEvolution evolution = PatternDiscoveryEngine.compare(baseline, current);

        // spring-field-injection was strengthened (score went from 0.0 to 0.5)
        // spring-constructor-injection was weakened (score went from 1.0 to 0.5)
        assertThat(evolution.strengthened()).extracting(Pattern::id).contains("spring-field-injection");
        assertThat(evolution.weakened()).extracting(Pattern::id).contains("spring-constructor-injection");
    }

    @Test
    public void shouldSupportQueryApis() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        Map<String, Object> serviceMeta = new HashMap<>();
        serviceMeta.put("annotations", List.of("Service"));
        RkgNode service = new RkgNode("com.example.UserService", RkgNodeType.CLASS, "UserService.java", serviceMeta, 1);
        graph.addNode(service);

        PatternDiscoveryEngine engine = new PatternDiscoveryEngine(graph);

        assertThat(engine.getNamingConventions()).isNotEmpty();
        assertThat(engine.getTestingStrategy()).isEmpty(); // No test classes added
        assertThat(engine.getDominantPatterns()).isNotEmpty(); // Suffix matching has score 1.0
    }
}

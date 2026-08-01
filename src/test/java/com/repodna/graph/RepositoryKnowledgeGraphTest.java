package com.repodna.graph;

import com.repodna.graph.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

public class RepositoryKnowledgeGraphTest {

    @Test
    public void shouldIndexAndQueryNodesCorrectly() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        Map<String, Object> meta1 = new HashMap<>();
        meta1.put("package", "com.example.service");
        meta1.put("annotations", List.of("Service", "Transactional"));
        RkgNode serviceNode = new RkgNode("com.example.service.UserService", RkgNodeType.CLASS, "UserService.java", meta1, 1);

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("package", "com.example.controller");
        meta2.put("annotations", List.of("RestController"));
        RkgNode controllerNode = new RkgNode("com.example.controller.UserController", RkgNodeType.CLASS, "UserController.java", meta2, 1);

        graph.addNode(serviceNode);
        graph.addNode(controllerNode);

        // Check lookups
        assertThat(graph.getNodeById("com.example.service.UserService")).isEqualTo(serviceNode);
        assertThat(graph.findNodesByType(RkgNodeType.CLASS)).containsExactlyInAnyOrder(serviceNode, controllerNode);
        assertThat(graph.findNodesByPackage("com.example.service")).containsExactly(serviceNode);
        assertThat(graph.findNodesByAnnotation("RestController")).containsExactly(controllerNode);
        assertThat(graph.findNodesByAnnotation("Transactional")).containsExactly(serviceNode);
    }

    @Test
    public void shouldDetectCircularDependencies() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        RkgNode a = new RkgNode("A", RkgNodeType.CLASS, "A.java", Collections.emptyMap(), 1);
        RkgNode b = new RkgNode("B", RkgNodeType.CLASS, "B.java", Collections.emptyMap(), 1);
        RkgNode c = new RkgNode("C", RkgNodeType.CLASS, "C.java", Collections.emptyMap(), 1);

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);

        // Cycle: A -> B -> C -> A
        graph.addEdge(new RkgEdge("A", "B", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));
        graph.addEdge(new RkgEdge("B", "C", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));
        graph.addEdge(new RkgEdge("C", "A", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));

        GraphQueryEngine queryEngine = new GraphQueryEngine(graph);
        List<String> cycles = queryEngine.findCircularDependencies();

        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0)).contains("A -> B -> C -> A");
    }

    @Test
    public void shouldFindDependencyChains() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        RkgNode x = new RkgNode("X", RkgNodeType.CLASS, "X.java", Collections.emptyMap(), 1);
        RkgNode y = new RkgNode("Y", RkgNodeType.CLASS, "Y.java", Collections.emptyMap(), 1);
        RkgNode z = new RkgNode("Z", RkgNodeType.CLASS, "Z.java", Collections.emptyMap(), 1);

        graph.addNode(x);
        graph.addNode(y);
        graph.addNode(z);

        graph.addEdge(new RkgEdge("X", "Y", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));
        graph.addEdge(new RkgEdge("Y", "Z", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));

        GraphQueryEngine queryEngine = new GraphQueryEngine(graph);
        List<List<String>> chains = queryEngine.findDependencyChains("X", "Z");

        assertThat(chains).hasSize(1);
        assertThat(chains.get(0)).containsExactly("X", "Y", "Z");
    }

    @Test
    public void shouldDetectOrphansAndUnusedClasses() {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();

        RkgNode orphan = new RkgNode("OrphanClass", RkgNodeType.CLASS, "OrphanClass.java", Collections.emptyMap(), 1);
        RkgNode referenced = new RkgNode("ReferencedClass", RkgNodeType.CLASS, "ReferencedClass.java", Collections.emptyMap(), 1);
        RkgNode caller = new RkgNode("CallerClass", RkgNodeType.CLASS, "CallerClass.java", Collections.emptyMap(), 1);

        graph.addNode(orphan);
        graph.addNode(referenced);
        graph.addNode(caller);

        graph.addEdge(new RkgEdge("CallerClass", "ReferencedClass", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));

        GraphQueryEngine queryEngine = new GraphQueryEngine(graph);

        // Orphan: has no incoming DEPENDS_ON / CALLS edges
        assertThat(queryEngine.findOrphanClasses()).containsExactlyInAnyOrder(orphan, caller);
        
        // Unused: has no incoming references from other classes
        assertThat(queryEngine.findUnusedClasses()).containsExactlyInAnyOrder(orphan, caller);
    }

    @Test
    public void shouldCompareGraphsCorrectly() {
        RepositoryKnowledgeGraph baseline = new RepositoryKnowledgeGraph();
        RepositoryKnowledgeGraph current = new RepositoryKnowledgeGraph();

        RkgNode node1 = new RkgNode("Class1", RkgNodeType.CLASS, "Class1.java", Collections.emptyMap(), 1);
        RkgNode node2 = new RkgNode("Class2", RkgNodeType.CLASS, "Class2.java", Collections.emptyMap(), 1);

        baseline.addNode(node1);
        baseline.addNode(node2);
        baseline.addEdge(new RkgEdge("Class1", "Class2", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));

        // Current snapshot: add Class3, remove Class2, change relationship
        RkgNode node3 = new RkgNode("Class3", RkgNodeType.CLASS, "Class3.java", Collections.emptyMap(), 1);
        current.addNode(node1);
        current.addNode(node3);
        current.addEdge(new RkgEdge("Class1", "Class3", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));

        GraphComparer.GraphComparisonReport report = GraphComparer.compare(baseline, current);

        assertThat(report.addedNodes()).containsExactly("Class3");
        assertThat(report.removedNodes()).containsExactly("Class2");
        assertThat(report.addedRelationships()).contains("Class1 -DEPENDS_ON-> Class3");
        assertThat(report.removedRelationships()).contains("Class1 -DEPENDS_ON-> Class2");
    }

    @Test
    public void shouldBeThreadSafeDuringConcurrentUpdates() throws InterruptedException {
        RepositoryKnowledgeGraph graph = new RepositoryKnowledgeGraph();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 100; i++) {
            final int id = i;
            executor.submit(() -> {
                RkgNode node = new RkgNode("Node" + id, RkgNodeType.CLASS, "File" + id + ".java", Collections.emptyMap(), 1);
                graph.addNode(node);
                graph.addEdge(new RkgEdge("Node" + id, "Target", RkgEdgeType.DEPENDS_ON, 1.0, Collections.emptyList(), null));
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        assertThat(graph.getAllNodes()).hasSize(100);
        assertThat(graph.getAllEdges()).hasSize(100);
    }
}

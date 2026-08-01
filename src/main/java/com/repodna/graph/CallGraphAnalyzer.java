package com.repodna.graph;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.alg.cycle.CycleDetector;
import java.util.*;

public class CallGraphAnalyzer {
    private final DefaultDirectedGraph<String, DefaultEdge> classGraph;
    
    public CallGraphAnalyzer(DefaultDirectedGraph<String, DefaultEdge> classGraph) {
        this.classGraph = classGraph;
    }
    
    /** Find classes involved in circular dependencies */
    public Set<String> findCircularDependencies() {
        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(classGraph);
        return detector.findCycles();
    }
    
    /** Get fan-in for each class (how many other classes depend on it) */
    public Map<String, Integer> getFanIn() {
        Map<String, Integer> fanIn = new HashMap<>();
        for (String v : classGraph.vertexSet()) {
            fanIn.put(v, classGraph.inDegreeOf(v));
        }
        return fanIn;
    }
    
    /** Get fan-out for each class (how many classes it depends on) */
    public Map<String, Integer> getFanOut() {
        Map<String, Integer> fanOut = new HashMap<>();
        for (String v : classGraph.vertexSet()) {
            fanOut.put(v, classGraph.outDegreeOf(v));
        }
        return fanOut;
    }
    
    /** Find God classes (high fan-in, top N) */
    public List<Map.Entry<String, Integer>> findGodClasses(int topN) {
        return getFanIn().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(topN)
            .toList();
    }
    
    /** Get total class count */
    public int getClassCount() { return classGraph.vertexSet().size(); }
    
    /** Get total dependency count */
    public int getDependencyCount() { return classGraph.edgeSet().size(); }
    
    /** Check if there are any cycles */
    public boolean hasCycles() {
        return new CycleDetector<>(classGraph).detectCycles();
    }
}

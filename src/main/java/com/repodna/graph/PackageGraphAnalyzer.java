package com.repodna.graph;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.alg.cycle.CycleDetector;
import java.util.*;

public class PackageGraphAnalyzer {
    private final DefaultDirectedGraph<String, DefaultEdge> packageGraph;
    
    public PackageGraphAnalyzer(DefaultDirectedGraph<String, DefaultEdge> packageGraph) {
        this.packageGraph = packageGraph;
    }
    
    /** Find package cycles */
    public Set<String> findPackageCycles() {
        return new CycleDetector<>(packageGraph).findCycles();
    }
    
    /** 
     * Calculate instability for each package.
     * Instability = Ce / (Ca + Ce) where Ca = afferent coupling (incoming), Ce = efferent coupling (outgoing)
     * Range: 0 (maximally stable) to 1 (maximally unstable)
     */
    public Map<String, Double> calculateInstability() {
        Map<String, Double> instability = new HashMap<>();
        for (String pkg : packageGraph.vertexSet()) {
            int ca = packageGraph.inDegreeOf(pkg);  // afferent
            int ce = packageGraph.outDegreeOf(pkg);  // efferent
            double total = ca + ce;
            instability.put(pkg, total == 0 ? 0.0 : (double) ce / total);
        }
        return instability;
    }
    
    /** Get afferent coupling (incoming dependencies) per package */
    public Map<String, Integer> getAfferentCoupling() {
        Map<String, Integer> result = new HashMap<>();
        for (String pkg : packageGraph.vertexSet()) {
            result.put(pkg, packageGraph.inDegreeOf(pkg));
        }
        return result;
    }
    
    /** Get efferent coupling (outgoing dependencies) per package */
    public Map<String, Integer> getEfferentCoupling() {
        Map<String, Integer> result = new HashMap<>();
        for (String pkg : packageGraph.vertexSet()) {
            result.put(pkg, packageGraph.outDegreeOf(pkg));
        }
        return result;
    }
    
    /** Get package count */
    public int getPackageCount() { return packageGraph.vertexSet().size(); }
    
    /** Check for cycles */
    public boolean hasCycles() {
        return new CycleDetector<>(packageGraph).detectCycles();
    }
}

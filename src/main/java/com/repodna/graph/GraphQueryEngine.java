package com.repodna.graph;

import com.repodna.graph.model.*;

import java.util.*;

/**
 * Executes deterministic query operations over the Repository Knowledge Graph.
 */
public class GraphQueryEngine {
    private final RepositoryKnowledgeGraph graph;

    public GraphQueryEngine(RepositoryKnowledgeGraph graph) {
        this.graph = graph;
    }

    /**
     * Finds all Controller nodes.
     */
    public List<RkgNode> findControllers() {
        List<RkgNode> controllers = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS) {
                @SuppressWarnings("unchecked")
                List<String> annos = (List<String>) node.metadata().get("annotations");
                if (annos != null && (annos.contains("RestController") || annos.contains("Controller"))) {
                    controllers.add(node);
                }
            }
        }
        return controllers;
    }

    /**
     * Finds all Service nodes.
     */
    public List<RkgNode> findServices() {
        List<RkgNode> services = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS) {
                @SuppressWarnings("unchecked")
                List<String> annos = (List<String>) node.metadata().get("annotations");
                if (annos != null && (annos.contains("Service") || annos.contains("Component"))) {
                    services.add(node);
                }
            }
        }
        return services;
    }

    /**
     * Finds all Repository nodes.
     */
    public List<RkgNode> findRepositories() {
        List<RkgNode> repositories = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS || node.type() == RkgNodeType.INTERFACE) {
                @SuppressWarnings("unchecked")
                List<String> annos = (List<String>) node.metadata().get("annotations");
                if (annos != null && annos.contains("Repository")) {
                    repositories.add(node);
                } else if (node.id().endsWith("Repository")) {
                    repositories.add(node);
                }
            }
        }
        return repositories;
    }

    /**
     * Finds caller nodes of a specific method.
     */
    public List<RkgNode> findCallers(String methodFqn) {
        List<RkgNode> callers = new ArrayList<>();
        for (RkgEdge edge : graph.getInEdges(methodFqn)) {
            if (edge.type() == RkgEdgeType.CALLS) {
                RkgNode callerNode = graph.getNodeById(edge.source());
                if (callerNode != null) {
                    callers.add(callerNode);
                }
            }
        }
        return callers;
    }

    /**
     * Finds dependency chains between two nodes.
     */
    public List<List<String>> findDependencyChains(String startId, String endId) {
        List<List<String>> chains = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        findChainsDfs(startId, endId, visited, chains);
        return chains;
    }

    private void findChainsDfs(String current, String target, Set<String> visited, List<List<String>> chains) {
        visited.add(current);
        if (current.equals(target)) {
            chains.add(new ArrayList<>(visited));
        } else {
            for (RkgEdge edge : graph.getOutEdges(current)) {
                if (edge.type() == RkgEdgeType.DEPENDS_ON || edge.type() == RkgEdgeType.CALLS) {
                    if (!visited.contains(edge.target())) {
                        findChainsDfs(edge.target(), target, visited, chains);
                    }
                }
            }
        }
        visited.remove(current);
    }

    /**
     * Detects circular dependencies within classes or interfaces.
     */
    public List<String> findCircularDependencies() {
        List<String> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS || node.type() == RkgNodeType.INTERFACE || node.type() == RkgNodeType.RECORD) {
                if (!visited.contains(node.id())) {
                    detectCycleDfs(node.id(), visited, stack, currentPath, cycles);
                }
            }
        }
        return cycles;
    }

    private void detectCycleDfs(String node, Set<String> visited, Set<String> stack, List<String> currentPath, List<String> cycles) {
        visited.add(node);
        stack.add(node);
        currentPath.add(node);

        for (RkgEdge edge : graph.getOutEdges(node)) {
            if (edge.type() == RkgEdgeType.DEPENDS_ON) {
                String target = edge.target();
                if (stack.contains(target)) {
                    // Cycle detected, format path
                    int index = currentPath.indexOf(target);
                    List<String> cyclePath = new ArrayList<>(currentPath.subList(index, currentPath.size()));
                    cyclePath.add(target);
                    cycles.add(String.join(" -> ", cyclePath));
                } else if (!visited.contains(target)) {
                    detectCycleDfs(target, visited, stack, currentPath, cycles);
                }
            }
        }

        currentPath.remove(node);
        stack.remove(node);
    }

    /**
     * Finds orphan classes (classes with no parent containment packages or incoming references).
     */
    public List<RkgNode> findOrphanClasses() {
        List<RkgNode> orphans = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS || node.type() == RkgNodeType.INTERFACE || node.type() == RkgNodeType.RECORD) {
                boolean hasIncoming = false;
                for (RkgEdge edge : graph.getInEdges(node.id())) {
                    if (edge.type() == RkgEdgeType.CONTAINS && edge.sourceLocation() != null) {
                        continue; // skip packages containers
                    }
                    if (edge.type() == RkgEdgeType.DEPENDS_ON || edge.type() == RkgEdgeType.CALLS) {
                        hasIncoming = true;
                        break;
                    }
                }
                if (!hasIncoming) {
                    orphans.add(node);
                }
            }
        }
        return orphans;
    }

    /**
     * Finds unused classes (classes not referenced by any other class or interface).
     */
    public List<RkgNode> findUnusedClasses() {
        List<RkgNode> unused = new ArrayList<>();
        for (RkgNode node : graph.getAllNodes()) {
            if (node.type() == RkgNodeType.CLASS || node.type() == RkgNodeType.INTERFACE || node.type() == RkgNodeType.RECORD) {
                // Check if any incoming edges are DEPENDS_ON or CALLS or IMPLEMENTS
                boolean isUsed = false;
                for (RkgEdge edge : graph.getInEdges(node.id())) {
                    if (edge.type() == RkgEdgeType.DEPENDS_ON || edge.type() == RkgEdgeType.CALLS 
                            || edge.type() == RkgEdgeType.IMPLEMENTS || edge.type() == RkgEdgeType.EXTENDS) {
                        if (!edge.source().equals(node.id())) { // ignore self dependency
                            isUsed = true;
                            break;
                        }
                    }
                }
                if (!isUsed) {
                    unused.add(node);
                }
            }
        }
        return unused;
    }
}

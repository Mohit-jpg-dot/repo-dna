package com.repodna.graph;

import com.repodna.graph.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Storage and indexing layer of the Repository Knowledge Graph (RKG).
 */
public class RepositoryKnowledgeGraph {
    private final GraphIndexManager indexManager = new GraphIndexManager();
    private final Map<String, List<RkgEdge>> outEdges = new ConcurrentHashMap<>();
    private final Map<String, List<RkgEdge>> inEdges = new ConcurrentHashMap<>();

    /**
     * Registers a node and updates indices.
     */
    public void addNode(RkgNode node) {
        indexManager.indexNode(node);
    }

    /**
     * Deregisters a node by ID and updates indices.
     */
    public void removeNode(String id) {
        RkgNode node = indexManager.getNodeById(id);
        if (node != null) {
            indexManager.deindexNode(node);
        }
        outEdges.remove(id);
        inEdges.remove(id);

        // Clean dangling relationships
        for (List<RkgEdge> edges : outEdges.values()) {
            edges.removeIf(e -> e.target().equals(id));
        }
        for (List<RkgEdge> edges : inEdges.values()) {
            edges.removeIf(e -> e.source().equals(id));
        }
    }

    /**
     * Registers a relationship edge.
     */
    public void addEdge(RkgEdge edge) {
        outEdges.computeIfAbsent(edge.source(), k -> new CopyOnWriteArrayList<>()).add(edge);
        inEdges.computeIfAbsent(edge.target(), k -> new CopyOnWriteArrayList<>()).add(edge);
    }

    public RkgNode getNodeById(String id) {
        return indexManager.getNodeById(id);
    }

    public List<RkgNode> findNodesByType(RkgNodeType type) {
        return indexManager.getNodesByType(type);
    }

    public List<RkgNode> findNodesByAnnotation(String annotationName) {
        return indexManager.getNodesByAnnotation(annotationName);
    }

    public List<RkgNode> findNodesByPackage(String packageName) {
        return indexManager.getNodesByPackage(packageName);
    }

    public List<RkgNode> findNodesBySourceFile(String sourceFile) {
        return indexManager.getNodesBySourceFile(sourceFile);
    }

    public Collection<RkgNode> getAllNodes() {
        return indexManager.getAllNodes();
    }

    public Collection<RkgEdge> getAllEdges() {
        List<RkgEdge> allEdges = new ArrayList<>();
        for (List<RkgEdge> edges : outEdges.values()) {
            allEdges.addAll(edges);
        }
        return Collections.unmodifiableCollection(allEdges);
    }

    public List<RkgEdge> getOutEdges(String sourceNodeId) {
        return outEdges.getOrDefault(sourceNodeId, Collections.emptyList());
    }

    public List<RkgEdge> getInEdges(String targetNodeId) {
        return inEdges.getOrDefault(targetNodeId, Collections.emptyList());
    }

    public void clear() {
        indexManager.clear();
        outEdges.clear();
        inEdges.clear();
    }
}

package com.repodna.graph;

import com.repodna.graph.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Automates safe indexing of Knowledge Graph entities for sub-millisecond querying.
 */
public class GraphIndexManager {
    private final Map<String, RkgNode> nodesById = new ConcurrentHashMap<>();
    private final Map<RkgNodeType, List<RkgNode>> nodesByType = new ConcurrentHashMap<>();
    private final Map<String, List<RkgNode>> nodesByPackage = new ConcurrentHashMap<>();
    private final Map<String, List<RkgNode>> nodesByAnnotation = new ConcurrentHashMap<>();
    private final Map<String, List<RkgNode>> nodesBySourceFile = new ConcurrentHashMap<>();

    /**
     * Re-indexes a newly added node.
     */
    public void indexNode(RkgNode node) {
        nodesById.put(node.id(), node);
        nodesByType.computeIfAbsent(node.type(), k -> new CopyOnWriteArrayList<>()).add(node);

        String pkg = (String) node.metadata().get("package");
        if (pkg != null) {
            nodesByPackage.computeIfAbsent(pkg, k -> new CopyOnWriteArrayList<>()).add(node);
        }

        @SuppressWarnings("unchecked")
        List<String> annotations = (List<String>) node.metadata().get("annotations");
        if (annotations != null) {
            for (String anno : annotations) {
                nodesByAnnotation.computeIfAbsent(anno, k -> new CopyOnWriteArrayList<>()).add(node);
            }
        }

        String sourceFile = (String) node.metadata().get("sourceFile");
        if (sourceFile != null) {
            nodesBySourceFile.computeIfAbsent(sourceFile, k -> new CopyOnWriteArrayList<>()).add(node);
        }
    }

    /**
     * Removes all references of a node from indices.
     */
    public void deindexNode(RkgNode node) {
        nodesById.remove(node.id());
        
        List<RkgNode> typeList = nodesByType.get(node.type());
        if (typeList != null) typeList.remove(node);

        String pkg = (String) node.metadata().get("package");
        if (pkg != null) {
            List<RkgNode> pkgList = nodesByPackage.get(pkg);
            if (pkgList != null) pkgList.remove(node);
        }

        @SuppressWarnings("unchecked")
        List<String> annotations = (List<String>) node.metadata().get("annotations");
        if (annotations != null) {
            for (String anno : annotations) {
                List<RkgNode> annoList = nodesByAnnotation.get(anno);
                if (annoList != null) annoList.remove(node);
            }
        }

        String sourceFile = (String) node.metadata().get("sourceFile");
        if (sourceFile != null) {
            List<RkgNode> fileList = nodesBySourceFile.get(sourceFile);
            if (fileList != null) fileList.remove(node);
        }
    }

    public RkgNode getNodeById(String id) {
        return nodesById.get(id);
    }

    public List<RkgNode> getNodesByType(RkgNodeType type) {
        return nodesByType.getOrDefault(type, Collections.emptyList());
    }

    public List<RkgNode> getNodesByPackage(String packageName) {
        return nodesByPackage.getOrDefault(packageName, Collections.emptyList());
    }

    public List<RkgNode> getNodesByAnnotation(String annotationName) {
        return nodesByAnnotation.getOrDefault(annotationName, Collections.emptyList());
    }

    public List<RkgNode> getNodesBySourceFile(String sourceFile) {
        return nodesBySourceFile.getOrDefault(sourceFile, Collections.emptyList());
    }

    public Collection<RkgNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodesById.values());
    }

    public void clear() {
        nodesById.clear();
        nodesByType.clear();
        nodesByPackage.clear();
        nodesByAnnotation.clear();
        nodesBySourceFile.clear();
    }
}

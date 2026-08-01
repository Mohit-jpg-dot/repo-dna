package com.repodna.graph;

import com.repodna.graph.model.RkgEdge;
import com.repodna.graph.model.RkgNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares two snapshots of the Repository Knowledge Graph to trace changes and structural evolution.
 */
public class GraphComparer {

    public record GraphComparisonReport(
        List<String> addedNodes,
        List<String> removedNodes,
        List<String> modifiedNodes,
        List<String> addedRelationships,
        List<String> removedRelationships
    ) {}

    /**
     * Compares baseline and current graphs, identifying added, removed, and modified elements.
     */
    public static GraphComparisonReport compare(RepositoryKnowledgeGraph baseline, RepositoryKnowledgeGraph current) {
        List<String> addedNodes = new ArrayList<>();
        List<String> removedNodes = new ArrayList<>();
        List<String> modifiedNodes = new ArrayList<>();

        for (RkgNode currNode : current.getAllNodes()) {
            RkgNode baseNode = baseline.getNodeById(currNode.id());
            if (baseNode == null) {
                addedNodes.add(currNode.id());
            } else if (!Objects.equals(currNode.metadata(), baseNode.metadata()) || currNode.type() != baseNode.type()) {
                modifiedNodes.add(currNode.id());
            }
        }

        for (RkgNode baseNode : baseline.getAllNodes()) {
            if (current.getNodeById(baseNode.id()) == null) {
                removedNodes.add(baseNode.id());
            }
        }

        List<String> addedEdges = new ArrayList<>();
        List<String> removedEdges = new ArrayList<>();

        for (RkgEdge currEdge : current.getAllEdges()) {
            String signature = getEdgeSignature(currEdge);
            boolean exists = false;
            for (RkgEdge baseEdge : baseline.getAllEdges()) {
                if (getEdgeSignature(baseEdge).equals(signature)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                addedEdges.add(signature);
            }
        }

        for (RkgEdge baseEdge : baseline.getAllEdges()) {
            String signature = getEdgeSignature(baseEdge);
            boolean exists = false;
            for (RkgEdge currEdge : current.getAllEdges()) {
                if (getEdgeSignature(currEdge).equals(signature)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                removedEdges.add(signature);
            }
        }

        return new GraphComparisonReport(addedNodes, removedNodes, modifiedNodes, addedEdges, removedEdges);
    }

    private static String getEdgeSignature(RkgEdge edge) {
        return edge.source() + " -" + edge.type() + "-> " + edge.target();
    }
}

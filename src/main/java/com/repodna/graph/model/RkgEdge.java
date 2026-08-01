package com.repodna.graph.model;

import java.util.List;

/**
 * Immutable model of a relationship edge in the Repository Knowledge Graph.
 */
public record RkgEdge(
    String source,
    String target,
    RkgEdgeType type,
    double confidence,
    List<String> evidence,
    String sourceLocation
) {}

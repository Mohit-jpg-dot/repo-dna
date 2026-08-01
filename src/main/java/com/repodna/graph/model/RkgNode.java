package com.repodna.graph.model;

import java.util.Map;

/**
 * Immutable model of an entity node in the Repository Knowledge Graph.
 */
public record RkgNode(
    String id,
    RkgNodeType type,
    String sourceLocation,
    Map<String, Object> metadata,
    int version
) {}

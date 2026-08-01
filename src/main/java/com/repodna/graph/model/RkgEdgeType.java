package com.repodna.graph.model;

/**
 * Types of relationships (edges) that connect nodes in the Repository Knowledge Graph.
 */
public enum RkgEdgeType {
    CONTAINS,
    CALLS,
    EXTENDS,
    IMPLEMENTS,
    USES_ANNOTATION,
    RETURNS,
    HAS_TYPE,
    IMPORTS,
    DEPENDS_ON,
    CREATES_BEAN
}

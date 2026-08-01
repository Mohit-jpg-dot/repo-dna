package com.repodna.parser.model;

/**
 * Immutable reference to a resolved or unresolved type.
 */
public record TypeReference(
    String name,
    String qualifiedName,
    boolean isResolved
) {}

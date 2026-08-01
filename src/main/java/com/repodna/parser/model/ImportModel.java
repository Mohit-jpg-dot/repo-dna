package com.repodna.parser.model;

/**
 * Immutable model of a package or type import specification.
 */
public record ImportModel(
    String qualifiedName,
    boolean isStatic,
    boolean isAsterisk
) {}

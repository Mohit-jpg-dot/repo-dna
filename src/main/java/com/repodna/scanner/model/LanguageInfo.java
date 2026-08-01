package com.repodna.scanner.model;

/**
 * Immutable representation of a programming language detected in the repository.
 */
public record LanguageInfo(
    String name,
    int fileCount,
    double percentage
) {}

package com.repodna.parser.model;

import java.util.List;

/**
 * Aggregate root containing the parsed structural semantics of a project.
 */
public record ProjectModel(
    String name,
    List<PackageModel> packages,
    List<String> unresolvedSymbols
) {}

package com.repodna.scanner.model;

import java.util.List;

/**
 * Immutable representation of documentation files found in the repository.
 */
public record DocumentationInfo(
    List<String> presentDocs
) {}

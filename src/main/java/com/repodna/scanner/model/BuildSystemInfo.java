package com.repodna.scanner.model;

import java.util.List;

/**
 * Immutable representation of the project's build system and package manager.
 */
public record BuildSystemInfo(
    String name,
    String packageManager,
    List<String> buildFiles
) {}

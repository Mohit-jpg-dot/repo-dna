package com.repodna.discovery.model;

import java.util.List;

/**
 * Representation of a grouped collection of related engineering patterns.
 */
public record PatternGroup(
    String groupId,
    String name,
    String description,
    List<Pattern> patterns
) {}

package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java class field.
 */
public record FieldModel(
    String name,
    TypeReference type,
    Set<String> modifiers,
    List<AnnotationModel> annotations,
    boolean hasInitializer,
    int line
) {}

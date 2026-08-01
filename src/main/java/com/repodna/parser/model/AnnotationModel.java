package com.repodna.parser.model;

import java.util.Map;

/**
 * Immutable model of an annotation instance decorating a code element.
 */
public record AnnotationModel(
    String name,
    String qualifiedName,
    Map<String, String> values
) {}

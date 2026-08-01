package com.repodna.parser.model;

import java.util.List;

/**
 * Immutable model of a method or constructor parameter.
 */
public record ParameterModel(
    String name,
    TypeReference type,
    List<AnnotationModel> annotations
) {}

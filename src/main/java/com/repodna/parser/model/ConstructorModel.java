package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java class constructor.
 */
public record ConstructorModel(
    String id,
    String name,
    List<ParameterModel> parameters,
    Set<String> modifiers,
    List<AnnotationModel> annotations,
    int startLine,
    int endLine,
    List<CallSiteModel> calls
) {}

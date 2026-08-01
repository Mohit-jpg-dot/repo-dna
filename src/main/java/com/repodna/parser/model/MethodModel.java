package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java method.
 */
public record MethodModel(
    String id,
    String name,
    TypeReference returnType,
    List<ParameterModel> parameters,
    Set<String> modifiers,
    List<AnnotationModel> annotations,
    int startLine,
    int endLine,
    List<CallSiteModel> calls,
    boolean isAbstract
) {}

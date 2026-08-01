package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java record type (Java 16+).
 */
public record RecordModel(
    String name,
    String qualifiedName,
    Set<String> modifiers,
    List<AnnotationModel> annotations,
    List<ParameterModel> components,
    List<TypeReference> interfaces,
    List<ConstructorModel> constructors,
    List<MethodModel> methods,
    int startLine,
    int endLine,
    String sourceFile
) {}

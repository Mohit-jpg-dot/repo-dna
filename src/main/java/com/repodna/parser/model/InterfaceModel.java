package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java interface type.
 */
public record InterfaceModel(
    String name,
    String qualifiedName,
    Set<String> modifiers,
    List<TypeReference> interfaces,
    List<AnnotationModel> annotations,
    List<MethodModel> methods,
    int startLine,
    int endLine,
    String sourceFile
) {}

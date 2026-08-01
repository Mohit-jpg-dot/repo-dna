package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java enum type.
 */
public record EnumModel(
    String name,
    String qualifiedName,
    Set<String> modifiers,
    List<AnnotationModel> annotations,
    List<String> entries,
    List<ConstructorModel> constructors,
    List<MethodModel> methods,
    List<FieldModel> fields,
    int startLine,
    int endLine,
    String sourceFile
) {}

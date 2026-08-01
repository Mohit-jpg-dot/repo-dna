package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Immutable model of a Java class type.
 */
public record ClassModel(
    String name,
    String qualifiedName,
    Set<String> modifiers,
    TypeReference superClass,
    List<TypeReference> interfaces,
    List<AnnotationModel> annotations,
    List<ConstructorModel> constructors,
    List<MethodModel> methods,
    List<FieldModel> fields,
    List<String> typeParameters,
    List<ClassModel> nestedClasses,
    int startLine,
    int endLine,
    String sourceFile
) {}

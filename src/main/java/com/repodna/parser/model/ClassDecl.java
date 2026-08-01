package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a Java class, interface, enum, record, or annotation declaration.
 */
public record ClassDecl(
    String name,
    ClassType classType,
    Set<String> modifiers,
    String superClass,               // null if none
    List<String> interfaces,         // implemented interfaces
    List<AnnotationDecl> annotations,
    List<MethodDecl> methods,
    List<FieldDecl> fields,
    List<ClassDecl> innerClasses,
    int startLine
) {
    /**
     * The type of the class-like declaration.
     */
    public enum ClassType {
        CLASS, INTERFACE, ENUM, RECORD, ANNOTATION
    }
}

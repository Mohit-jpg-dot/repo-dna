package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a Java field declaration.
 */
public record FieldDecl(
    String name,
    String type,              // e.g., "UserService", "List<User>"
    Set<String> modifiers,    // e.g., {"private", "final"}
    List<AnnotationDecl> annotations,
    boolean hasInitializer,
    int startLine
) {}

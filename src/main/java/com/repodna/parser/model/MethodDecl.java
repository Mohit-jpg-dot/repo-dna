package com.repodna.parser.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a Java method or constructor declaration.
 */
public record MethodDecl(
    String name,
    String returnType,             // e.g., "void", "ResponseEntity<User>"
    List<ParameterInfo> parameters,
    Set<String> modifiers,
    List<AnnotationDecl> annotations,
    int bodyLineCount,             // number of lines in the method body
    List<String> methodCalls,      // names of methods called within this method
    boolean isConstructor,
    int startLine
) {
    /**
     * Represents a parameter in a method or constructor.
     */
    public record ParameterInfo(String name, String type, List<AnnotationDecl> annotations) {}
}

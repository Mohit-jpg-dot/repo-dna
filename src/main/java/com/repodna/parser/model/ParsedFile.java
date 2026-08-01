package com.repodna.parser.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a parsed Java source file.
 */
public record ParsedFile(
    Path filePath,
    String packageName,
    List<ImportDecl> imports,
    List<ClassDecl> classes
) {
    /** Get the primary (first/public) class */
    public ClassDecl primaryClass() {
        return classes.isEmpty() ? null : classes.getFirst();
    }
    
    /** Get fully qualified name of the primary class */
    public String fullyQualifiedName() {
        ClassDecl primary = primaryClass();
        if (primary == null) return null;
        return packageName != null ? packageName + "." + primary.name() : primary.name();
    }
}

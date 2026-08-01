package com.repodna.parser.model;

/**
 * Represents a Java import declaration.
 */
public record ImportDecl(
    String qualifiedName,    // e.g., "org.springframework.web.bind.annotation.RestController"
    boolean isStatic,
    boolean isWildcard
) {}

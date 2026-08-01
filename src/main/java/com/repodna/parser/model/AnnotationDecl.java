package com.repodna.parser.model;

import java.util.Map;

/**
 * Represents a Java annotation declaration.
 */
public record AnnotationDecl(
    String name,                    // e.g., "RestController", "Service"
    Map<String, String> attributes  // e.g., {"value": "/api/users"}
) {}

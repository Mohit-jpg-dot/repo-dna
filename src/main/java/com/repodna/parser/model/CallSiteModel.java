package com.repodna.parser.model;

/**
 * Immutable representation of a method call site.
 */
public record CallSiteModel(
    String calleeName,
    String calleeTypeFqn,
    String methodName,
    int line
) {}

package com.repodna.graph.model;

/**
 * Types of entities that can exist as nodes in the Repository Knowledge Graph.
 */
public enum RkgNodeType {
    REPOSITORY,
    MODULE,
    PACKAGE,
    DIRECTORY,
    SOURCE_FILE,
    CLASS,
    INTERFACE,
    ENUM,
    RECORD,
    ANNOTATION,
    METHOD,
    CONSTRUCTOR,
    FIELD,
    PARAMETER,
    IMPORT,
    DEPENDENCY,
    FRAMEWORK_COMPONENT,
    CONFIGURATION_FILE,
    BUILD_FILE,
    DOCUMENTATION_FILE,
    GIT_METADATA,
    TEST
}

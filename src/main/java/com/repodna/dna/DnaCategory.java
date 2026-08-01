package com.repodna.dna;

public enum DnaCategory {
    NAMING("Naming Conventions"),
    ARCHITECTURE("Architecture & Layering"),
    TESTING("Testing Practices"),
    SECURITY("Security Patterns"),
    ERROR_HANDLING("Error Handling"),
    LOGGING("Logging"),
    VALIDATION("Validation"),
    DATA_ACCESS("Data Access & Transactions"),
    REST_API("REST API Design"),
    DEPENDENCY_MANAGEMENT("Dependency Management"),
    CACHING("Caching"),
    CONFIGURATION("Configuration & Spring");
    
    private final String displayName;
    DnaCategory(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    
    /** Map pattern category strings to DnaCategory enum */
    public static DnaCategory fromPatternCategory(String category) {
        if (category == null) return ARCHITECTURE;
        return switch (category.toLowerCase()) {
            case "naming" -> NAMING;
            case "architecture", "layering", "package-organization" -> ARCHITECTURE;
            case "testing" -> TESTING;
            case "security" -> SECURITY;
            case "exception-handling", "error-handling" -> ERROR_HANDLING;
            case "logging" -> LOGGING;
            case "validation" -> VALIDATION;
            case "transaction", "data-access", "dto" -> DATA_ACCESS;
            case "rest", "rest-api" -> REST_API;
            case "dependency", "dependency-management" -> DEPENDENCY_MANAGEMENT;
            case "caching" -> CACHING;
            case "spring", "configuration" -> CONFIGURATION;
            default -> ARCHITECTURE; // fallback
        };
    }
}

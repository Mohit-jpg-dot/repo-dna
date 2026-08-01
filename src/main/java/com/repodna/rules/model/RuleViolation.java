package com.repodna.rules.model;

import java.nio.file.Path;

public record RuleViolation(
    Path filePath,
    int lineNumber,
    String description,
    String suggestion
) {}

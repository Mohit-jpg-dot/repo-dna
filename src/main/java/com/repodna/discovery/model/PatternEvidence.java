package com.repodna.discovery.model;

import java.nio.file.Path;

public record PatternEvidence(
    Path filePath,
    int lineNumber,
    String snippet,
    String explanation
) {}

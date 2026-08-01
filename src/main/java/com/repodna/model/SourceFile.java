package com.repodna.model;

import java.nio.file.Path;
import java.time.Instant;

public record SourceFile(
    Path path,
    Path absolutePath,
    FileType type,
    String language,
    long sizeBytes,
    Instant lastModified
) {
    public enum FileType {
        SOURCE, TEST, CONFIG, BUILD, RESOURCE, DOCUMENTATION, DEPENDENCY_MANIFEST, OTHER
    }
}
